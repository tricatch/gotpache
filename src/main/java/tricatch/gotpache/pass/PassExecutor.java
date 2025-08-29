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
import tricatch.gotpache.util.ByteUtils;
import tricatch.gotpache.util.SocketUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class PassExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(PassExecutor.class);

    private Socket clientSocket = null;
    private HttpStreamReader clientIn = null;
    private HttpStreamWriter clientOut = null;

    private Socket serverSocket = null;
    private HttpStreamReader serverIn = null;
    private HttpStreamWriter serverOut = null;

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

            if (logger.isTraceEnabled()){
                logger.trace("read from socket - h{}", clientSocket.hashCode());
            }

            clientIn = new HttpStreamReader(clientSocket.getInputStream());
            clientOut = new HttpStreamWriter(clientSocket.getOutputStream());

            //read-req-header
            HeaderLines requestHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);
            int bytesRead = clientIn.readHeaders(requestHeaders, HTTP.MAX_HEADER_LENGTH);
            
            if (bytesRead == -1) {
                logger.warn("No headers received from client");
                return;
            }
            
            // Parse HTTP request
            HttpRequest httpRequest = requestHeaders.parseHttpRequest();
            logger.debug("Parsed request: {}", httpRequest);
            
            // Log request details
            logger.info("Request: {} {} {} (Host: {}, Connection: {}, ContentLength: {}, Body: {})", 
                       httpRequest.getMethod(), 
                       httpRequest.getPath(), 
                       httpRequest.getVersion(),
                       httpRequest.getHost(),
                       httpRequest.getConnection(),
                       httpRequest.getContentLength(),
                       httpRequest.getBodyStream());

            //create socket - url matched
            serverSocket = createServerSocket(httpRequest.getHost(), httpRequest.getPath());
            serverIn = new HttpStreamReader(serverSocket.getInputStream());
            serverOut = new HttpStreamWriter(serverSocket.getOutputStream());

            serverOut.writeHeaders(requestHeaders);

            //read-res-header
            HeaderLines responseHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);

            //read-res-header
            bytesRead = serverIn.readHeaders(responseHeaders, HTTP.MAX_HEADER_LENGTH);

            if (bytesRead == -1) {
                logger.warn("No headers received from server");
                return;
            }

            //parse-res-header
            HttpResponse response = responseHeaders.parseHttpResponse();
            logger.debug("Parsed response: {}", response);

            //write-res-header
            clientOut.writeHeaders(responseHeaders);

            // Relay response body to client
            relayResponseBody(response);

            // //write-req-header
            // serverOut.writeHeader(requestHeader);

            // //read-res-header
            // ResponseHeader responseHeader = serverIn.readHeader();
            // BodyStream bodyStream = serverIn.getBodyStream(responseHeader);

            // //write-res-header
            // clientOut.writeHeader(responseHeader);

            // logger.debug( "buffer - index ( {} - {} ), full-raw\n{}"
            //         , serverIn.getBufferPos()
            //         , serverIn.getBufferEnd()
            //         , ByteUtils.toHexPretty(serverIn.getBuffer(), 0,  serverIn.getBufferEnd())
            //         );


            // for(int i1=0;;i1++) {
            //     //read-chunk-size
            //     ChunkSize chunkSize = serverIn.readChunkSize();

            //     //write-chunk-size
            //     clientOut.writeChunkSize(chunkSize);

            //     if( chunkSize.size == 0 ){
            //         ChunkEnd chunkEnd = serverIn.readChunkEnd();
            //         clientOut.writeChunkEnd(chunkEnd);
            //         break;
            //     }

            //     for (int i2=0;;i2++) {

            //         logger.trace("----- {}/{} -----", i1, i2);

            //         //read-chunk-stream
            //         ChunkStream chunkStream = serverIn.readChunkStream(chunkSize);

            //         //write-chunk-stream
            //         clientOut.writeChunkStream(chunkStream);

            //         if( chunkStream.last ) break;
            //     }
            // }


            logger.debug("END");

        } catch (IOException e) {
            logger.error( e.getMessage(), e);
        } finally {
            closeAll();
        }


    }

    private void closeAll(){

        if( logger.isTraceEnabled() ) logger.trace( "close" );

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

//    /**
//     * WebSocket 핸드셰이크를 처리합니다.
//     */
//    private void handleWebSocketHandshake() throws IOException {
//        if( logger.isDebugEnabled() ) logger.debug( "WebSocket handshake processing" );
//
//        // WebSocket 핸드셰이크 응답이 제대로 전달되었는지 확인
//        // 서버 응답에서 WebSocket 업그레이드 응답을 클라이언트로 전달
//        // 이미 pipeResponseHeader()에서 처리되었으므로 추가 작업 불필요
//
//        // WebSocket 연결이 성공적으로 설정되었는지 확인
//        if (logger.isDebugEnabled()) {
//            logger.debug("WebSocket handshake completed successfully");
//        }
//    }
//
//    /**
//     * WebSocket 프레임을 처리합니다.
//     */
//    private void handleWebSocketFrames() throws IOException {
//        if (logger.isDebugEnabled()) logger.debug("WebSocket frame processing started");
//
//        // 클라이언트에서 서버로 데이터 중계
//        Thread clientToServer = new Thread(() -> {
//            try {
//                InputStream clientIn = clientSocket.getInputStream();
//                WebSocketUtil.relay(clientIn, serverOut, "C->S");
//                if (logger.isDebugEnabled()) {
//                    logger.debug("WebSocket C->S relay completed");
//                }
//            } catch (IOException e) {
//                logger.debug("WebSocket C->S relay failed: {}", e.getMessage(), e);
//            }
//        }, "WebSocket-C2S");
//
//        // 서버에서 클라이언트로 데이터 중계
//        Thread serverToClient = new Thread(() -> {
//            try {
//                WebSocketUtil.relay(serverIn, clientOut, "S->C");
//                if (logger.isDebugEnabled()) {
//                    logger.debug("WebSocket S->C relay completed");
//                }
//            } catch (IOException e) {
//                logger.debug("WebSocket S->C relay failed: {}", e.getMessage(), e);
//            }
//        }, "WebSocket-S2C");
//
//        clientToServer.start();
//        serverToClient.start();
//
//        logger.debug("WebSocket bidirectional relay started");
//
//        try {
//            clientToServer.join();
//            serverToClient.join();
//        } catch (InterruptedException e) {
//            logger.debug("WebSocket threads interrupted: {}", e.getMessage(), e);
//            Thread.currentThread().interrupt();
//        }
//
//        logger.debug("WebSocket frame processing completed");
//    }

    /**
     * Relay response body to client based on body stream type
     * @param response HTTP response containing body stream information
     * @throws IOException when I/O error occurs
     */
    private void relayResponseBody(HttpResponse response) throws IOException {
        BodyStream bodyStream = response.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("Relaying response body with type: {}", bodyStream);
        }
        
        switch (bodyStream) {
            case NONE:
            case NULL:
                // No body to relay
                if (logger.isTraceEnabled()) {
                    logger.trace("No response body to relay");
                }
                break;
                
            case CONTENT_LENGTH:
                relayContentLengthBody(response);
                break;
                
            case CHUNKED:
                relayChunkedBody();
                break;
                
            case WEBSOCKET:
                // WebSocket upgrade response - no body to relay
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket upgrade response - no body to relay");
                }
                break;
                
            case UNTIL_CLOSE:
                relayUntilCloseBody();
                break;
                
            default:
                logger.warn("Unknown body stream type: {}", bodyStream);
                break;
        }
    }
    
    /**
     * Relay content-length based response body
     * @param response HTTP response
     * @throws IOException when I/O error occurs
     */
    private void relayContentLengthBody(HttpResponse response) throws IOException {
        Integer contentLength = response.getContentLength();
        if (contentLength == null || contentLength <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("No content length or zero content length");
            }
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("Relaying content-length body: {} bytes", contentLength);
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int remainingBytes = contentLength;
        
        while (remainingBytes > 0) {
            int bytesToRead = Math.min(buffer.length, remainingBytes);
            int bytesRead = serverIn.read(buffer, 0, bytesToRead);
            
            if (bytesRead == -1) {
                logger.warn("Unexpected end of stream while reading content-length body");
                break;
            }
            
            clientOut.write(buffer, 0, bytesRead);
            remainingBytes -= bytesRead;
            
            if (logger.isTraceEnabled()) {
                logger.trace("Relayed {} bytes, remaining: {}", bytesRead, remainingBytes);
            }
        }
        
        clientOut.flush();
        if (logger.isDebugEnabled()) {
            logger.debug("Content-length body relay completed");
        }
    }
    
    /**
     * Relay chunked transfer encoding response body
     * @throws IOException when I/O error occurs
     */
    private void relayChunkedBody() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Relaying chunked response body");
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        
        while (true) {
            // Read chunk size line
            ByteBuffer chunkSizeBuffer = new ByteBuffer(new byte[128]);
            int bytesRead = serverIn.readLine(chunkSizeBuffer, 128);
            
            if (bytesRead == -1) {
                logger.warn("Unexpected end of stream while reading chunk size");
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
                logger.error("Invalid chunk size: {}", chunkSizeLine);
                break;
            }

            if (logger.isTraceEnabled()) {
                logger.trace("Chunk size: {}", chunkSize);
            }

            // Write chunk size to client
            clientOut.write(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            clientOut.write(HTTP.CRLF);
            
            if (chunkSize == 0) {
                // End of chunked body - read and relay trailer headers
                if (logger.isDebugEnabled()) {
                    logger.debug("End of chunked body (chunk size 0) - reading trailer headers");
                }
                
                clientOut.write(HTTP.CRLF);
                clientOut.flush();
                break;
            }
            
            // Relay chunk data
            int remainingBytes = chunkSize;
            while (remainingBytes > 0) {
                int bytesToRead = Math.min(buffer.length, remainingBytes);
                bytesRead = serverIn.read(buffer, 0, bytesToRead);
                
                if (bytesRead == -1) {
                    logger.warn("Unexpected end of stream while reading chunk data");
                    break;
                }
                clientOut.write(buffer, 0, bytesRead);
                clientOut.flush();
                remainingBytes -= bytesRead;
                
                if (logger.isTraceEnabled()) {
                    logger.trace("Relayed {} bytes of chunk, remaining: {}", bytesRead, remainingBytes);
                }
            }
            
            // Read and relay chunk end (CRLF)
            int cr = serverIn.read();
            int lf = serverIn.read();
            if (cr == '\r' && lf == '\n') {
                clientOut.write(HTTP.CRLF);
                clientOut.flush();
            } else {
                logger.warn("Invalid chunk end marker");
                break;
            }
        }
        
        clientOut.flush();
        if (logger.isDebugEnabled()) {
            logger.debug("Chunked body relay completed");
        }
    }
    
    /**
     * Relay response body until connection close
     * @throws IOException when I/O error occurs
     */
    private void relayUntilCloseBody() throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("Relaying until-close response body");
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
            
            if (logger.isTraceEnabled()) {
                logger.trace("Relayed {} bytes, total: {}", bytesRead, totalBytesRelayed);
            }
        }
        
        clientOut.flush();
        if (logger.isDebugEnabled()) {
            logger.debug("Until-close body relay completed, total bytes: {}", totalBytesRelayed);
        }
    }

}
