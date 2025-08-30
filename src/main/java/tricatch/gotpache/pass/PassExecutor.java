package tricatch.gotpache.pass;

import io.github.azagniotov.matcher.AntPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.BodyStream;
import tricatch.gotpache.http.io.ByteBuffer;
import tricatch.gotpache.http.io.HeaderLines;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpResponse;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.server.VirtualPath;
import tricatch.gotpache.util.SocketUtils;
import tricatch.gotpache.util.SysUtil;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class PassExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PassExecutor.class);

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

    public PassExecutor(Socket clientSocket, VirtualHosts virtualHosts, int connectTimeout, int readTimeout){

        this.clientSocket = clientSocket;
        this.virtualHosts = virtualHosts;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public void run() {

        String rid = SysUtil.generateRequestId();

        try {

            clientIn = new HttpStreamReader(clientSocket.getInputStream(), HTTP.BODY_BUFFER_SIZE);
            clientOut = new HttpStreamWriter(clientSocket.getOutputStream());

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
            
           if( logger.isDebugEnabled() ) {
               logger.debug("{}, Request Headers\n{}"
                       , rid
                       , requestHeaders
               );
               logger.debug("{}, Request: {} {} {} (Host: {}, Body: {}, Connection: {}, ContentLength: {})"
                       , rid
                       , httpRequest.getMethod()
                       , httpRequest.getPath()
                       , httpRequest.getVersion()
                       , httpRequest.getHost()
                       , httpRequest.getBodyStream()
                       , httpRequest.getConnection()
                       , httpRequest.getContentLength()
               );
           }

            //create socket - url matched
            serverSocket = createServerSocket(rid, httpRequest.getHost(), httpRequest.getPath());
            serverIn = new HttpStreamReader(serverSocket.getInputStream(), HTTP.BODY_BUFFER_SIZE);
            serverOut = new HttpStreamWriter(serverSocket.getOutputStream());

            //write-req-header
            serverOut.writeHeaders(requestHeaders);

            //read-res-header
            HeaderLines responseHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);

            //read-res-header
            bytesRead = serverIn.readHeaders(responseHeaders, HTTP.MAX_HEADER_LENGTH);

            if (bytesRead == -1) {
                logger.warn("{}, No headers received from server"
                        , rid
                );
                return;
            }

            //parse-res-header
            HttpResponse response = responseHeaders.parseHttpResponse();
            if( logger.isDebugEnabled() ) {
                logger.debug("{}, Response Headers\n{}"
                        , rid
                        , responseHeaders
                );
                logger.debug("{}, Response: {} {} {} (Body: {}, Connection: {}, ContentLength: {})"
                        , rid
                        , response.getVersion()
                        , response.getStatusCode()
                        , response.getStatusMessage()
                        , response.getBodyStream()
                        , response.getConnection()
                        , response.getContentLength()
                );
            }

            //write-res-header
            clientOut.writeHeaders(responseHeaders);

            // Relay response body to client
            relayResponseBody(rid, response);

        } catch (IOException e) {
            logger.error( rid + ", " + e.getMessage(), e);
        } finally {
            closeAll(rid);
        }


    }

    private void closeAll(String rid){

        if( logger.isDebugEnabled() ){
            logger.debug( "{}, Close", rid );
        }

        if( clientIn !=null ) try{  clientIn.close(); }catch (Exception e){}
        if( clientOut !=null ) try{ clientOut.close(); }catch(Exception e){}
        if( clientSocket !=null ) try{ clientSocket.close(); }catch(Exception e){}

        if( serverIn !=null ) try{ serverIn.close(); }catch(Exception e){}
        if( serverOut !=null ) try{ serverOut.close(); }catch(Exception e){}
        if( serverSocket !=null ) try{ serverSocket.close(); }catch(Exception e){}

        clientIn = null;
        clientOut = null;
        clientSocket = null;

        serverIn = null;
        serverOut = null;
        serverSocket = null;
    }

    private Socket createServerSocket(String rid, String vhost, String uri) throws IOException {

        if( logger.isDebugEnabled() ){
            logger.debug( "{}, Create Socket, vhost={}, uri={}"
                    , rid
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
                    logger.debug("{}, Reserved path - {}, {}, {}, {}"
                            , rid
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
                logger.debug("{}, Create HTTPS {}:{} / {}"
                        , rid
                        , target.getHost()
                        , port
                        , vhost
                );
            }
            return SocketUtils.createHttps(vhost, target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        } else {
            int port = target.getPort() <= 0 ? 80 : target.getPort();
            if( logger.isDebugEnabled() ) logger.debug("{}, Create HTTP {}:{} / {}"
                    , rid
                    , target.getHost()
                    , port
                    , vhost
            );
            return SocketUtils.createHttp(target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        }
    }

    /**
     * Relay response body to client based on body stream type
     * @param response HTTP response containing body stream information
     * @throws IOException when I/O error occurs
     */
    private void relayResponseBody(String rid, HttpResponse response) throws IOException {
        BodyStream bodyStream = response.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, Relaying response body with type: {}"
                    , rid
                    , bodyStream
            );
        }
        
        switch (bodyStream) {
            case NONE:
            case NULL:
                // No-body to relay
                if (logger.isDebugEnabled()) {
                    logger.trace("{}, No response body to relay"
                            , rid
                    );
                }
                break;
                
            case CONTENT_LENGTH:
                relayContentLengthBody(rid, response);
                break;
                
            case CHUNKED:
                relayChunkedBody(rid);
                break;
                
            case WEBSOCKET:
                // WebSocket upgrade response - no body to relay
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, WebSocket upgrade response - no body to relay"
                            , rid
                    );
                }
                break;
                
            case UNTIL_CLOSE:
                relayUntilCloseBody(rid);
                break;
                
            default:
                logger.warn("{}, Unknown body stream type: {}"
                        , bodyStream
                        , rid
                );
                break;
        }
    }
    
    /**
     * Relay content-length based response body
     * @param response HTTP response
     * @throws IOException when I/O error occurs
     */
    private void relayContentLengthBody(String rid, HttpResponse response) throws IOException {
        Integer contentLength = response.getContentLength();
        if (contentLength == null || contentLength <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}, No content length or zero content length"
                        , rid
                );
            }
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, Relaying content-length body: {} bytes"
                    , rid
                    , contentLength
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int remainingBytes = contentLength;
        
        while (remainingBytes > 0) {
            int bytesToRead = Math.min(buffer.length, remainingBytes);
            int bytesRead = serverIn.read(buffer, 0, bytesToRead);
            
            if (bytesRead == -1) {
                logger.warn("{}, Unexpected end of stream while reading content-length body"
                        , rid
                );
                break;
            }
            
            clientOut.write(buffer, 0, bytesRead);
            remainingBytes -= bytesRead;
            
            if (logger.isDebugEnabled()) {
                logger.debug("{}, Relayed {} bytes, remaining: {}"
                        , rid
                        , bytesRead
                        , remainingBytes
                );
            }
        }
        
        clientOut.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, Content-length body relay completed"
                    , rid
            );
        }
    }
    
    /**
     * Relay chunked transfer encoding response body
     * @throws IOException when I/O error occurs
     */
    private void relayChunkedBody(String rid) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, Relaying chunked response body"
                    , rid
            );
        }

        ByteBuffer chunkSizeBuffer = new ByteBuffer(HTTP.CHUNK_SIZE_LINE_LENGTH);
        byte[] chunkBodyBuffer = new byte[HTTP.BODY_BUFFER_SIZE];
        
        while (true) {
            // Read chunk size line
            int bytesRead = serverIn.readLine(chunkSizeBuffer, HTTP.CHUNK_SIZE_LINE_LENGTH);
            
            if (bytesRead == -1) {
                logger.warn("{}, Unexpected end of stream while reading chunk size"
                        , rid
                );
                break;
            }
            
            String chunkSizeLine = new String(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            int semicolonIndex = chunkSizeLine.indexOf(';');
            if (semicolonIndex > 0) {
                chunkSizeLine = chunkSizeLine.substring(0, semicolonIndex);
            }
            
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
            } catch (NumberFormatException e) {
                logger.error("{}, Invalid chunk size: {}"
                        , rid
                        , chunkSizeLine
                );
                break;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("{}, Chunk size: {}"
                        , rid
                        , chunkSize
                );
            }

            // Write chunk size to client
            clientOut.write(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            clientOut.write(HTTP.CRLF);
            
            if (chunkSize == 0) {
                // End of chunked body - read and relay trailer headers
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, End of chunked body (chunk size 0) - reading trailer headers"
                            , rid
                    );
                }
                
                clientOut.write(HTTP.CRLF);
                clientOut.flush();
                break;
            }
            
            // Relay chunk data
            int remainingBytes = chunkSize;
            while (remainingBytes > 0) {
                int bytesToRead = Math.min(chunkBodyBuffer.length, remainingBytes);
                bytesRead = serverIn.read(chunkBodyBuffer, 0, bytesToRead);
                
                if (bytesRead == -1) {
                    logger.warn("{}, Unexpected end of stream while reading chunk data"
                            , rid
                    );
                    break;
                }
                clientOut.write(chunkBodyBuffer, 0, bytesRead);
                clientOut.flush();
                remainingBytes -= bytesRead;
                
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, Relayed {} bytes of chunk, remaining: {}"
                            , rid
                            , bytesRead
                            , remainingBytes
                    );
                }
            }
            
            // Read and relay chunk end (CR-LF)
            int cr = serverIn.read();
            int lf = serverIn.read();
            if (cr == '\r' && lf == '\n') {
                clientOut.write(HTTP.CRLF);
                clientOut.flush();
            } else {
                logger.warn("{}, Invalid chunk end marker", rid);
                break;
            }
        }
        
        clientOut.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, Chunked body relay completed"
                    , rid
            );
        }
    }
    
    /**
     * Relay response body until connection close
     * @throws IOException when I/O error occurs
     */
    private void relayUntilCloseBody(String rid) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, Relaying until-close response body"
                    , rid
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int totalBytesRelayed = 0;
        
        while (true) {
            int bytesRead = serverIn.read(buffer);
            
            if (bytesRead == -1) {
                // End of stream
                break;
            }
            
            clientOut.write(buffer, 0, bytesRead);
            totalBytesRelayed += bytesRead;
            
            if (logger.isDebugEnabled()) {
                logger.debug("{}, Relayed {} bytes, total: {}"
                        , rid
                        , bytesRead
                        , totalBytesRelayed
                );
            }
        }
        
        clientOut.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, Until-close body relay completed, total bytes: {}"
                    , rid
                    , totalBytesRelayed
            );
        }
    }

}
