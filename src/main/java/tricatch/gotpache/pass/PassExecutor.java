package tricatch.gotpache.pass;

import io.github.azagniotov.matcher.AntPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpInputStream;
import tricatch.gotpache.http.io.ServerResponse;
import tricatch.gotpache.util.ByteUtils;
import tricatch.gotpache.util.SocketUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class PassExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(PassExecutor.class);


    private Socket clientSocket = null;
    private OutputStream clientOut = null;
    private PassClientRequest clientReq = null;

    private Socket serverSocket = null;
    private OutputStream serverOut = null;
    private InputStream serverIn = null;
    private ServerResponse serverRes = null;

    private VirtualHosts virtualHosts;
    private int connectTimeout = 0;
    private int readTimeout = 0;

    public PassExecutor(Socket clientSocket, VirtualHosts virtualHosts, int connectTimeout, int readTimeout){

        this.clientSocket = clientSocket;
        this.virtualHosts = virtualHosts;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    @Override
    public void run() {

        try {

            if (logger.isTraceEnabled()) logger.trace("read from socket - h{}", this.clientSocket.hashCode());

            this.clientOut = this.clientSocket.getOutputStream();

            //100. reader header from client
            this.clientReq = readClinetRequest(this.clientSocket.getInputStream());

            this.serverSocket = createServerSocket();
            this.serverIn = this.serverSocket.getInputStream();
            this.serverOut = this.serverSocket.getOutputStream();

            //200. write header to server
            pipeRequestHeader();

            //300. read body from client and write to server
            pipeRequestBody();

            //400. read header from server
            this.serverRes = readServerResponse(this.serverIn);

            //500. write header to client
            pipeResponseHeader();

            //600. read body from server and write to client
            pipeResponseBody();


        } catch (IOException e) {
            logger.error( e.getMessage(), e);
        } finally {
            closeAll();
        }


    }

    private void closeAll(){

        if( logger.isTraceEnabled() ) logger.trace( "close" );

        if( clientReq !=null ) clientReq.close();
        if( clientOut !=null ) try{ clientOut.close(); }catch(Exception e){}
        if( clientSocket !=null ) try{ clientSocket.close(); }catch(Exception e){}

        if( serverRes !=null ) serverRes.close();
        if( serverIn !=null ) try{ serverIn.close(); }catch(Exception e){}
        if( serverOut !=null ) try{ serverOut.close(); }catch(Exception e){}
        if( serverSocket !=null ) try{ serverSocket.close(); }catch(Exception e){}

        clientReq = null;
        clientOut = null;
        clientSocket = null;

        serverRes = null;
        serverIn = null;
        serverOut = null;
        serverSocket = null;
    }

    private PassClientRequest readClinetRequest(InputStream clientIn) throws IOException {

        if( logger.isTraceEnabled() ) logger.trace( "C-REQ-H-Read" );

        PassClientRequest cr = new PassClientRequest(clientIn);

        if( logger.isDebugEnabled() ) logger.debug( "C-REQ-Header\n{}", new String(cr.getHeaderBuffer(), 0, cr.getEndOfHeader()) );

        return cr;
    }

    private ServerResponse readServerResponse(InputStream serverIn) throws IOException {

        if( logger.isTraceEnabled() ) logger.trace( "S-RES-H-Read" );

        ServerResponse sr = new ServerResponse(serverIn);

        if( logger.isDebugEnabled() ) logger.debug( "S-RES-Header - {}\n{}", this.clientReq.getUri(), new String(sr.getHeaderBuffer(), 0, sr.getEndOfHeader()) );

        return sr;
    }

    private Socket createServerSocket() throws IOException {

        String vhost = this.clientReq.getHost();
        String uri = this.clientReq.getUri();

        List<VirtualPath> urls = this.virtualHosts.get(vhost);

        if( urls==null || urls.size()==0 ) throw new IOException( "undefined vhost - " + vhost );

        VirtualPath virtualPath = null;

        for (int i = 0; i < urls.size(); i++) {
            VirtualPath vu = urls.get(i);
            AntPathMatcher pathMatcher = new AntPathMatcher.Builder().build();
            boolean matched = pathMatcher.isMatch( vu.getPath(), uri );

            logger.debug( "url matched - {}, {}, {}", matched, vu.getPath(), uri );

            if( matched ){
                virtualPath = vu;
                if( logger.isDebugEnabled()){
                    logger.debug( "url matched - {}{}, {}{}", vhost, vu.getPath(), vu.getTarget(), uri );
                }
                break;
            }
        }


        if( virtualPath ==null ) throw new IOException( "Not found pattern - " + uri + " -- " + vhost );

//        if( logger.isDebugEnabled() ) logger.debug( "S-REQ-Socket open - {} to {}:{}/SSL={}", this.clientReq.getMethod()
//                , .getTargetHost()
//                , .getTargetPort()
//                , .isSsl()
//        );


        URL target = virtualPath.getTarget();

        if( "https".equals(target.getProtocol()) ){
            return SocketUtils.createHttps(vhost, target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        } else {
            return SocketUtils.createHttp(target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        }
    }

    private void pipeRequestHeader() throws IOException {

        if( logger.isTraceEnabled() ) logger.trace( "S-REQ-H-Write" );

        List<byte[]> reqHeaders = this.clientReq.getHeaders();

        for(int i=0;i<reqHeaders.size();i++){

            byte[] bufLine = reqHeaders.get(i);
            String strLine = new String(bufLine).trim().toLowerCase();

            if( strLine.startsWith(HTTP.HEADER_PROXY_CONNECTION) ) continue;
            if( strLine.startsWith(HTTP.HEADER_UPGRADE_INSECURE_REQUEST) ) continue;
            //if( strLine.startsWith(HTTP.HEADER_ACCEPT_ENCODING) ) continue;

            if( strLine.startsWith(HTTP.HEADER_CONNECTION) ) bufLine = HTTP.BUF_HEADER_CONNECTION_CLOSE;

            //logger.info( "S-REQ-H-W-{} {}", String.format("%02d", i+1), new String(bufLine).trim() );

            if( logger.isTraceEnabled() ) logger.trace( "S-REQ-H-W-{} {}", String.format("%02d", i+1), new String(bufLine).trim() );

            serverOut.write(bufLine);
        }
        serverOut.flush();
    }

    private void pipeRequestBody() throws IOException {

        int clen = this.clientReq.getContentLength();

        if( logger.isTraceEnabled() ) logger.trace( "S-REQ-B-Write-Pipe, Content-Length: {}", clen );

        if( clen==0 ) return;
        if( clen>0 ) pipeContentLenth( "S-REQ-B-W", clen, this.clientReq, this.serverOut);
    }



    private void pipeResponseHeader() throws IOException {

        if( logger.isTraceEnabled() ) logger.trace( "C-RES-H-Write" );

        List<byte[]> resHeaders = this.serverRes.getHeaders();

        for(int i=0;i<resHeaders.size();i++){

            byte[] line = resHeaders.get(i);

            //logger.info( "C-RES-H-W-{} {}", String.format("%02d", i+1), new String(line).trim() );

            if( logger.isTraceEnabled() ) logger.trace( "C-RES-H-W-{} {}", String.format("%02d", i+1), new String(line).trim() );

            this.clientOut.write(line);
        }
        this.clientOut.flush();
    }

    private void pipeResponseBody() throws IOException {

        if( logger.isTraceEnabled()){
            if( this.serverRes.isChunked() ) logger.trace( "C-RES-B-Write-Chunked" );
            else logger.trace( "C-RES-B-Write-Content-Length: {}", serverRes.getContentLength() );
        }

        if( serverRes.isChunked() ){
            pipeContentChunked( "C-RES-B-W", this.serverRes, this.clientOut);
            return;
        }

        int resContentLength = serverRes.getContentLength();

        if( resContentLength==0 ) return;

        pipeContentLenth( "C-RES-B-W", resContentLength, this.serverRes, this.clientOut);
    }

    private void pipeContentLenth(String step, int contentLength, HttpInputStream in, OutputStream out) throws IOException {

        int readBytes = 0;
        byte[] buf = new byte[HTTP.BODY_BUFFER_SIZE];

        for (;;) {

            int n = in.read(buf);
            if (n < 0) break;

            readBytes = readBytes + n;

            if( logger.isTraceEnabled() ) logger.trace("{}-{}/{}", step, readBytes, contentLength);

            out.write(buf, 0, n);
            out.flush();

            if ( contentLength> 0 && readBytes >= contentLength ) break;
        }
    }

    private void pipeContentChunked(String step, HttpInputStream in, OutputStream out) throws IOException {

        int readBytes = 0;
        byte[] lenBuf = new byte[8];
        int endOfLen = 0;
        byte[] buf = new byte[HTTP.BODY_BUFFER_SIZE];

        for (;;) {

            endOfLen = 0;
            for(int i=0;i<lenBuf.length;i++){

                int b = in.read();
                if( b<0 ) throw new IOException( "Invalid chunked stream" );

                lenBuf[endOfLen] = (byte)b;
                endOfLen++;

                if( b==10 ) break;
            }

            if( logger.isTraceEnabled() ){
                logger.trace( "{}-Checked L{}, {}", step, endOfLen, ByteUtils.cut(lenBuf, 0, endOfLen) );
            }
            out.write( lenBuf, 0, endOfLen );

            String hexChunkedLen = new String(lenBuf,0,endOfLen).trim();

            int chunkedLen = 0;

            if( !"".equals(hexChunkedLen) ) chunkedLen = Integer.parseInt( hexChunkedLen, 16 );

            if( logger.isTraceEnabled() ) logger.trace( "{}-Checked-Length: {} (h{})", step, chunkedLen, hexChunkedLen);

            if( chunkedLen>0 ) {

                readBytes = 0;

                for (; ; ) {

                    int max = chunkedLen - readBytes;
                    if (max > buf.length) max = buf.length;

                    int n = in.read(buf, 0, max);
                    if (n < 0) break;

                    readBytes = readBytes + n;

                    if (logger.isTraceEnabled())
                        logger.trace("{}-{}/{} {}", step, n, readBytes, ByteUtils.cut(buf, 0, n));

                    out.write(buf, 0, n);
                    out.flush();

                    if (readBytes >= chunkedLen) break;
                }
            }

            int cr = in.read();
            int lf = in.read();

            if( cr==13 && lf==10 ){

                if( logger.isTraceEnabled() ){
                    logger.trace( "{}-Checked L{}, {}", step, 2, HTTP.CRLF );
                }
                out.write(HTTP.CRLF);
                out.flush();
            }

            if( chunkedLen==0 ) break;
        }
    }

}
