package tricatch.gotpache.pass;

import io.github.azagniotov.matcher.AntPathMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.field.Chunk;
import tricatch.gotpache.http.io.*;
import tricatch.gotpache.util.ByteUtils;
import tricatch.gotpache.util.SocketUtils;

import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.List;

public class PassExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(PassExecutor.class);


    private Socket clientSocket = null;
    private HttpBufferedReader clientIn = null;
    private HttpBufferedWriter clientOut = null;

    private Socket serverSocket = null;
    private HttpBufferedWriter serverOut = null;
    private HttpBufferedReader serverIn = null;
    //private ServerResponse serverRes = null;

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

            clientIn = new HttpBufferedReader(StreamMode.REQUEST, clientSocket.getInputStream());
            clientOut = new HttpBufferedWriter(StreamMode.RESPONSE, clientSocket.getOutputStream());

           int readReqHeaderLen = clientIn.readHeader();
           if(logger.isDebugEnabled() ){
               logger.debug( "read-reg-header-length={}", readReqHeaderLen);
           }

            //create socket - url matched
            serverSocket = createServerSocket(clientIn.getHost(), clientIn.getPath());
            serverIn = new HttpBufferedReader(StreamMode.RESPONSE, serverSocket.getInputStream());
            serverOut = new HttpBufferedWriter(StreamMode.REQUEST, serverSocket.getOutputStream());

            if(logger.isDebugEnabled() ){
                logger.debug( "write-reg-header-length={}", readReqHeaderLen);
            }
            serverOut.write(clientIn.getHeaderBuffer(), 0, readReqHeaderLen);

            int readResHeaderLen = serverIn.readHeader();
            if(logger.isDebugEnabled() ){
                logger.debug( "read-res-header-length={}", readResHeaderLen);
            }

            if(logger.isDebugEnabled() ){
                logger.debug( "write-res-header-length={}", readResHeaderLen);
            }
            clientOut.write(serverIn.getHeaderBuffer(), 0, serverIn.getHeaderBufferLen());

            if( BodyMode.CHUNKED == serverIn.getBodyMode() ){

                int writeLen = 0;

                for(;;) {

                    if( logger.isTraceEnabled() ){
                        logger.trace("body-buffer\n{}"
                                , ByteUtils.toHexPretty(this.serverIn.getBodyBuffer()
                                        , this.serverIn.getBodyBufferPos()
                                        , this.serverIn.getBodyBufferLen()
                                        )
                        );
                    }

                    Chunk chunk = serverIn.readChunk();
                    int readChunkLength = 0;
                    if(logger.isDebugEnabled()){
                        logger.debug("chunk-size : " + chunk.size);
                    }

                    //chunk-size-block
                    writeLen = chunk.end - chunk.start + 2; // CRLF
                    clientOut.write(serverIn.getBodyBuffer()
                            , serverIn.getBodyBufferPos()
                            , writeLen
                    );
                    serverIn.moveBodyBufferPosBy(writeLen);

                    //complete-chunk
                    if( chunk.size == 0 ){
                        writeLen = 2; //CRLF
                        clientOut.write(serverIn.getBodyBuffer()
                                , serverIn.getBodyBufferPos()
                                , writeLen
                        );
                        clientOut.flush();
                        serverIn.moveBodyBufferPosBy(writeLen);
                        break;
                    }

                    //copy-remain-buffer
                    int remainBuffer = serverIn.getBodyBufferLen() - serverIn.getBodyBufferPos();
                    writeLen = Math.min(remainBuffer, chunk.size);
                    if (writeLen > 0) {
                        readChunkLength += writeLen;
                        clientOut.write(serverIn.getBodyBuffer()
                                , serverIn.getBodyBufferPos()
                                , writeLen
                        );
                        serverIn.moveBodyBufferPosBy(writeLen);
                    }

                    //end-of-chunk
                    if( readChunkLength >= chunk.size ){
                        writeLen = 2; //CRLF
                        clientOut.write(serverIn.getBodyBuffer()
                                , serverIn.getBodyBufferPos()
                                , writeLen
                        );
                        serverIn.moveBodyBufferPosBy(writeLen);
                    }

                    clientOut.flush();
                }
            }

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

}
