package tricatch.gotpache.pass;

import io.github.azagniotov.matcher.AntPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpStream;
import tricatch.gotpache.http.io.HeaderLines;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;
import tricatch.gotpache.server.VThreadExecutor;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.server.VirtualPath;
import tricatch.gotpache.util.SocketUtils;
import tricatch.gotpache.util.SysUtil;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

public class PassRequestExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PassRequestExecutor.class);

    private Socket clientSocket;
    private HttpStreamReader clientIn = null;
    private HttpStreamWriter clientOut = null;

    private Socket serverSocket = null;
    private HttpStreamReader serverIn = null;
    private HttpStreamWriter serverOut = null;

    private final VirtualHosts virtualHosts;
    private final int connectTimeout;
    private final int readTimeout;

    private VirtualPath virtualPath = null;

    private boolean stop = false;

    private Thread thisThread = null;
    private Thread child = null;

    private final String uid = SysUtil.generateRequestId();
    private String rid = uid;
    private int reqCounter = 0;

    public PassRequestExecutor(Socket clientSocket, VirtualHosts virtualHosts, int connectTimeout, int readTimeout){

        this.clientSocket = clientSocket;
        this.virtualHosts = virtualHosts;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public void setStop(boolean stop){
        this.stop = stop;
    }

    public boolean isStop(){
        return this.stop;
    }

    public String getUid(){
        return this.uid;
    }
    public String getRid(){
        return this.rid;
    }

    public Thread getThread(){
        return this.thisThread;
    }

    @Override
    public void run() {

        try {

            if( logger.isDebugEnabled() ){
                logger.debug( "{}, vtStart", this.uid );
            }

            thisThread = Thread.currentThread();
            clientIn = new HttpStreamReader(clientSocket.getInputStream(), HTTP.BODY_BUFFER_SIZE);
            clientOut = new HttpStreamWriter(clientSocket.getOutputStream());

            while (true) {

                reqCounter++;
                this.rid = this.uid + "-" + reqCounter;

                //read-req-header
                HeaderLines requestHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);
                int bytesRead = clientIn.readHeaders(requestHeaders, HTTP.MAX_HEADER_LENGTH);

                if (bytesRead == -1) {
                    logger.warn("{}, No headers received from client"
                            , rid
                    );
                    return;
                }

                // Parse HTTP request
                HttpRequest httpRequest = requestHeaders.parseHttpRequest();

                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Request Headers\n{}"
                            , rid
                            , HttpStream.Flow.REQ
                            , requestHeaders
                    );
                    logger.debug("{}, {}, Request: {} {} {} (Host: {}, Body: {}, Connection: {}, ContentLength: {})"
                            , rid
                            , HttpStream.Flow.REQ
                            , httpRequest.getMethod()
                            , httpRequest.getPath()
                            , httpRequest.getVersion()
                            , httpRequest.getHost()
                            , httpRequest.getHttpStream()
                            , httpRequest.getConnection()
                            , httpRequest.getContentLength()
                    );
                }

                //create socket - url matched
                if (serverSocket == null) {
                    serverSocket = createServerSocket(rid, httpRequest.getHost(), httpRequest.getPath());
                    serverIn = new HttpStreamReader(serverSocket.getInputStream(), HTTP.BODY_BUFFER_SIZE);
                    serverOut = new HttpStreamWriter(serverSocket.getOutputStream());

                    child =  VThreadExecutor.run(
                            new PassResponseExecutor(this, serverIn, clientOut)
                            , Thread.currentThread().getName() + "x"
                        );
                }

                //write-req-header
                serverOut.writeHeaders(requestHeaders);

                if (HttpStream.WEBSOCKET == httpRequest.getHttpStream()) {
                    this.clientSocket.setSoTimeout(1000 * 60 * 5);
                    this.serverSocket.setSoTimeout(1000 * 60 * 5);
                }

                // Relay request body to server if exists
                HttpStream.Connection connection = RelayBody.relayRequestBody(rid, HttpStream.Flow.REQ, httpRequest, clientIn, serverOut);
                if (connection == HttpStream.Connection.CLOSE) {
                    this.stop = true;
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, stop={} or park"
                            , rid
                            , HttpStream.Flow.REQ
                            , this.stop
                    );
                }

                if (this.stop) {
                    break;
                }

                LockSupport.park();
            }
        } catch (SocketTimeoutException e){
            logger.error( uid + ", " + e.getMessage());
        } catch (IOException e) {
            logger.error( uid + ", " + e.getMessage(), e);
        } finally {
            this.stop = true;
            closeAll();
        }


    }

    private void closeAll(){

        if( logger.isDebugEnabled() ){
            logger.debug( "{}, vtEnd & closeSocket, vtRes={}", this.uid, child != null ? child.getName() : "none" );
        }

        if( serverIn !=null ) try{ serverIn.close(); }catch(Exception e){ logger.debug("Error closing serverIn: {}", e.getMessage()); }
        if( serverOut !=null ) try{ serverOut.close(); }catch(Exception e){ logger.debug("Error closing serverOut: {}", e.getMessage()); }
        if( serverSocket !=null ) try{ serverSocket.close(); }catch(Exception e){ logger.debug("Error closing serverSocket: {}", e.getMessage()); }

        if( clientIn !=null ) try{  clientIn.close(); }catch (Exception e){ logger.debug("Error closing clientIn: {}", e.getMessage()); }
        if( clientOut !=null ) try{ clientOut.close(); }catch(Exception e){ logger.debug("Error closing clientOut: {}", e.getMessage()); }
        if( clientSocket !=null ) try{ clientSocket.close(); }catch(Exception e){ logger.debug("Error closing clientSocket: {}", e.getMessage()); }

        serverIn = null;
        serverOut = null;
        serverSocket = null;

        clientIn = null;
        clientOut = null;
        clientSocket = null;
    }

    private Socket createServerSocket(String rid, String vhost, String uri) throws IOException {

        if( logger.isDebugEnabled() ){
            logger.debug( "{}, {}, Create Socket, vhost={}, uri={}"
                    , rid
                    , HttpStream.Flow.REQ
                    , vhost
                    , uri
            );
        }

        List<VirtualPath> urls = this.virtualHosts.get(vhost);

        if( urls==null || urls.isEmpty()) throw new IOException( "Undefined vhost - " + vhost );

        for (VirtualPath vu : urls) {
            AntPathMatcher pathMatcher = new AntPathMatcher.Builder().build();
            boolean matched = pathMatcher.isMatch(vu.getPath(), uri);

            if (matched) {
                virtualPath = vu;
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Reserved path - {}, {}, {}, {}"
                            , rid
                            , HttpStream.Flow.REQ
                            , vhost
                            , vu.getPath()
                            , vu.getTarget()
                            , uri
                    );
                }
                break;
            }
        }

        if( virtualPath == null ) throw new IOException( "Not found path - " + uri + " -- " + vhost );

        URL target = virtualPath.getTarget();

        if( "https".equals(target.getProtocol()) ){
            int port = target.getPort() <= 0 ? 443 : target.getPort();
            if( logger.isDebugEnabled() ){
                logger.debug("{}, {}, Create HTTPS {}:{} / {}"
                        , rid
                        , HttpStream.Flow.REQ
                        , target.getHost()
                        , port
                        , vhost
                );
            }
            return SocketUtils.createHttps(vhost, target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        } else {
            int port = target.getPort() <= 0 ? 80 : target.getPort();
            if( logger.isDebugEnabled() ) logger.debug("{}, {}, Create HTTP {}:{} / {}"
                    , rid
                    , HttpStream.Flow.REQ
                    , target.getHost()
                    , port
                    , vhost
            );
            return SocketUtils.createHttp(target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        }
    }

}
