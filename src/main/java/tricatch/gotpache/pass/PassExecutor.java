package tricatch.gotpache.pass;

import io.github.azagniotov.matcher.AntPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.ReaderMode;
import tricatch.gotpache.http.HttpMode;
import tricatch.gotpache.http.io.HttpBufferedReader;
import tricatch.gotpache.http.io.HttpInputStream;
import tricatch.gotpache.http.io.ServerResponse;
import tricatch.gotpache.util.ByteUtils;
import tricatch.gotpache.util.SocketUtils;
import tricatch.gotpache.util.WebSocketUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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

    private VirtualPath virtualPath = null;

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

            HttpBufferedReader httpReqBufferedReader = new HttpBufferedReader(ReaderMode.REQUEST, this.clientSocket.getInputStream());

           int readReqHeaderLen = httpReqBufferedReader.readHeader();
           if(logger.isDebugEnabled() ) logger.debug( "read-reg-header-length={}", readReqHeaderLen);

            //create socket - url matched
            this.serverSocket = createServerSocket(httpReqBufferedReader.getHost(), httpReqBufferedReader.getPath());
            this.serverIn = this.serverSocket.getInputStream();
            this.serverOut = this.serverSocket.getOutputStream();

            if(logger.isDebugEnabled() ) logger.debug( "write-reg-header-length={}", readReqHeaderLen);
            this.serverOut.write(httpReqBufferedReader.getHeaderBuffer(), 0, readReqHeaderLen);

            HttpBufferedReader httpResBufferedReader = new HttpBufferedReader(ReaderMode.RESPONSE, this.serverIn);

            int readResHeaderLen = httpResBufferedReader.readHeader();
            if(logger.isDebugEnabled() ) logger.debug( "read-res-header-length={}", readResHeaderLen);


            if( true ) throw new IOException("XXXXX" );

//            logger.trace("write headers");
//            serverOut.flush();
//
//            logger.trace("read res-headers");
//            HttpBufferedReader httpResBufferedReader = new HttpBufferedReader(this.serverIn);
//
//            List<byte[]> resHeaders = httpResBufferedReader.readHeader();
//

            if( true ) throw new IOException("XXXXX" );

            //200. write header to server
            HttpMode httpMode = pipeRequestHeader();

            //300. read body from client and write to server
            pipeRequestBody();

            //400. read header from server
            this.serverRes = readServerResponse(this.serverIn);

            //500. write header to client
            pipeResponseHeader();

            //600. read body from server and write to client
            if( HttpMode.HTTP == httpMode ) pipeResponseBody();

            //700. Websocket
            if( HttpMode.WEBSOCKET == httpMode ){
                if( logger.isDebugEnabled() ) logger.debug( "WebSocket mode - start WebSocket protocol handling" );

                this.clientSocket.setSoTimeout( 60000 * 30 );
                this.serverSocket.setSoTimeout( 60000 * 30 );

                // WebSocket 핸드셰이크 처리
                handleWebSocketHandshake();

                // WebSocket 프레임 처리
                handleWebSocketFrames();

                if( logger.isDebugEnabled() ) logger.debug( "WebSocket mode - WebSocket protocol handling completed" );
            }

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

    private Socket createServerSocket(String vhost, String uri) throws IOException {

        if( logger.isDebugEnabled() ) logger.debug( "req, vhost={}, uri={}", vhost, uri);

        List<VirtualPath> urls = this.virtualHosts.get(vhost);

        if( urls==null || urls.size()==0 ) throw new IOException( "undefined vhost - " + vhost );

        for (int i = 0; i < urls.size(); i++) {
            VirtualPath vu = urls.get(i);
            AntPathMatcher pathMatcher = new AntPathMatcher.Builder().build();
            boolean matched = pathMatcher.isMatch( vu.getPath(), uri );

            if( logger.isDebugEnabled() ) logger.debug( "url matched - {}, {}, {}", matched, vu.getPath(), uri );

            if( matched ){
                virtualPath = vu;
                if( logger.isDebugEnabled()){
                    logger.debug( "url matched - {}{}, {}{}", vhost, vu.getPath(), vu.getTarget(), uri );
                }
                break;
            }
        }

        if( virtualPath == null ) throw new IOException( "Not found pattern - " + uri + " -- " + vhost );

        URL target = virtualPath.getTarget();

        if( "https".equals(target.getProtocol()) ){
            return SocketUtils.createHttps(vhost, target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        } else {
            return SocketUtils.createHttp(target.getHost(), target.getPort(), this.connectTimeout, this.readTimeout);
        }
    }

    private HttpMode pipeRequestHeader() throws IOException {

        if( logger.isTraceEnabled() ) logger.trace( "S-REQ-H-Write" );

        HttpMode httpMode = HttpMode.HTTP;
        List<byte[]> reqHeaders = this.clientReq.getHeaders();

        for(int i=0;i<reqHeaders.size();i++){

            byte[] bufLine = reqHeaders.get(i);
            String strLine = new String(bufLine).trim().toLowerCase();

            if( strLine.startsWith(HTTP.HEADER_PROXY_CONNECTION) ) continue;
            if( strLine.startsWith(HTTP.HEADER_UPGRADE_INSECURE_REQUEST) ) continue;
            //if( strLine.startsWith(HTTP.HEADER_ACCEPT_ENCODING) ) continue;
            if( strLine.equalsIgnoreCase(HTTP.HEADER_CONNECTION_UPGRADE) ){
                //bypass
                httpMode = HttpMode.WEBSOCKET;
            } else if( strLine.startsWith(HTTP.HEADER_CONNECTION) ) bufLine = HTTP.BUF_HEADER_CONNECTION_CLOSE;

            //logger.info( "S-REQ-H-W-{} {}", String.format("%02d", i+1), new String(bufLine).trim() );

            if( logger.isTraceEnabled() ) logger.trace( "S-REQ-H-W-{} {}", String.format("%02d", i+1), new String(bufLine).trim() );

            serverOut.write(bufLine);
        }
        serverOut.flush();

        return httpMode;
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

        for(int resHeaderIdx=0;resHeaderIdx<resHeaders.size();resHeaderIdx++){

            byte[] line = resHeaders.get(resHeaderIdx);
            String sline = new String(line);
            boolean removed = false;

            if( resHeaderIdx>0 && this.virtualPath != null && !this.virtualPath.getRemoveHeader().isEmpty()) {
                List<String> removeHeader = this.virtualPath.getRemoveHeader();
                for(int i=0; i<removeHeader.size();i++){
                    String rmHeader = removeHeader.get(i);
                    if( sline.toLowerCase().startsWith(rmHeader.toLowerCase()) ){
                        removed = true;
                        break;
                    }
                }
            }

            if( logger.isTraceEnabled() ){
                String rmmsg = removed ? "REMOVED" : "";
                logger.trace( "C-RES-H-W-{} {} {}", String.format("%02d", resHeaderIdx+1), sline, rmmsg );
            }

            if( removed ) continue;

            this.clientOut.write(line);

            //add headers at virtualhost conf after status line
            if( resHeaderIdx==0 && this.virtualPath != null && !this.virtualPath.getAddHeader().isEmpty() ){
                List<String> addHeader = this.virtualPath.getAddHeader();
                for (int fhidx = 0; fhidx < addHeader.size(); fhidx++) {
                    String header = addHeader.get(fhidx);
                    line = header.getBytes(StandardCharsets.UTF_8);
                    if( logger.isTraceEnabled() ){
                        logger.trace( "C-RES-H-W-{}..{} {}", String.format("%02d", resHeaderIdx+1), fhidx+1, header );
                    }
                    this.clientOut.write(line);
                    this.clientOut.write(HTTP.CRLF);
                }
            }

        }

        this.clientOut.flush();

        // WebSocket 응답인 경우 헤더 전송 완료 로깅
        if (logger.isDebugEnabled()) {
            String responseHeaders = new String(this.serverRes.getHeaderBuffer(), 0, this.serverRes.getEndOfHeader());
            if (responseHeaders.toLowerCase().contains("upgrade: websocket")) {
                logger.debug("WebSocket response headers sent to client");
            }
        }
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

    /**
     * WebSocket 핸드셰이크를 처리합니다.
     */
    private void handleWebSocketHandshake() throws IOException {
        if( logger.isDebugEnabled() ) logger.debug( "WebSocket handshake processing" );

        // WebSocket 핸드셰이크 응답이 제대로 전달되었는지 확인
        // 서버 응답에서 WebSocket 업그레이드 응답을 클라이언트로 전달
        // 이미 pipeResponseHeader()에서 처리되었으므로 추가 작업 불필요

        // WebSocket 연결이 성공적으로 설정되었는지 확인
        if (logger.isDebugEnabled()) {
            logger.debug("WebSocket handshake completed successfully");
        }
    }

    /**
     * WebSocket 프레임을 처리합니다.
     */
    private void handleWebSocketFrames() throws IOException {
        if (logger.isDebugEnabled()) logger.debug("WebSocket frame processing started");

        // 클라이언트에서 서버로 데이터 중계
        Thread clientToServer = new Thread(() -> {
            try {
                InputStream clientIn = clientSocket.getInputStream();
                WebSocketUtil.relay(clientIn, serverOut, "C->S");
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket C->S relay completed");
                }
            } catch (IOException e) {
                logger.debug("WebSocket C->S relay failed: {}", e.getMessage(), e);
            }
        }, "WebSocket-C2S");

        // 서버에서 클라이언트로 데이터 중계
        Thread serverToClient = new Thread(() -> {
            try {
                WebSocketUtil.relay(serverIn, clientOut, "S->C");
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket S->C relay completed");
                }
            } catch (IOException e) {
                logger.debug("WebSocket S->C relay failed: {}", e.getMessage(), e);
            }
        }, "WebSocket-S2C");

        clientToServer.start();
        serverToClient.start();

        logger.debug("WebSocket bidirectional relay started");

        try {
            clientToServer.join();
            serverToClient.join();
        } catch (InterruptedException e) {
            logger.debug("WebSocket threads interrupted: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        logger.debug("WebSocket frame processing completed");
    }

}
