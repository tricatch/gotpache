package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.BodyStream;
import tricatch.gotpache.http.io.ByteBuffer;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpResponse;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;

import java.io.IOException;

/**
 * Static class for handling HTTP response body relay operations
 */
public class RelayBody {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayBody.class);
    
    /**
     * Relay HTTP body to client/server based on body stream type
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param response HTTP response containing body stream information
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    public static void relayResponseBody(String rid, BodyStream.Flow flow, HttpResponse response, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        BodyStream bodyStream = response.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying body with type: {}"
                    , rid
                    , flow
                    , bodyStream
            );
        }
        
        switch (bodyStream) {
            case NONE:
            case NULL:
                // No-body to relay
                if (logger.isDebugEnabled()) {
                    logger.trace("{}, {}, No body to relay"
                            , rid
                            , flow
                    );
                }
                break;
                
            case CONTENT_LENGTH:
                relayContentLengthBody(rid, flow, response, in, out);
                break;
                
            case CHUNKED:
                relayChunkedBody(rid, flow, in, out);
                break;
                
            case WEBSOCKET:
                // WebSocket upgrade response - no body to relay
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, WebSocket upgrade - no body to relay"
                            , rid
                            , flow
                    );
                }
                break;
                
            case UNTIL_CLOSE:
                relayUntilCloseBody(rid, flow, in, out);
                break;
                
            default:
                logger.warn("{}, {}, Unknown body stream type: {}"
                        , rid
                        , flow
                        , bodyStream
                );
                break;
        }
    }
    
    /**
     * Relay request body to server based on body stream type
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param request HTTP request containing body stream information
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    public static void relayResponseBody(String rid, BodyStream.Flow flow, HttpRequest request, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        BodyStream bodyStream = request.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying body with type: {}"
                    , rid
                    , flow
                    , bodyStream
            );
        }
        
        switch (bodyStream) {
            case NONE:
            case NULL:
                // No-body to relay
                if (logger.isDebugEnabled()) {
                    logger.trace("{}, {}, No body to relay"
                            , rid
                            , flow
                    );
                }
                break;
                
            case CONTENT_LENGTH:
                relayContentLengthBody(rid, flow, request, in, out);
                break;
                
            case CHUNKED:
                relayChunkedBody(rid, flow, in, out);
                break;
                
            case WEBSOCKET:
                // WebSocket upgrade request - no body to relay
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, WebSocket upgrade - no body to relay"
                            , rid
                            , flow
                    );
                }
                break;
                
            case UNTIL_CLOSE:
                relayUntilCloseBody(rid, flow, in, out);
                break;
                
            default:
                logger.warn("{}, {}, Unknown body stream type: {}"
                        , rid
                        , flow
                        , bodyStream
                );
                break;
        }
    }
    
    /**
     * Relay content-length based response body
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param response HTTP response
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    private static void relayContentLengthBody(String rid, BodyStream.Flow flow, HttpResponse response, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        Integer contentLength = response.getContentLength();
        if (contentLength == null || contentLength <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, No content length or zero content length"
                        , rid
                        , flow
                );
            }
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying content-length body: {} bytes"
                    , rid
                    , flow
                    , contentLength
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int remainingBytes = contentLength;
        
        while (remainingBytes > 0) {
            int bytesToRead = Math.min(buffer.length, remainingBytes);
            int bytesRead = in.read(buffer, 0, bytesToRead);
            
            if (bytesRead == -1) {
                logger.warn("{}, {}, Unexpected end of stream while reading content-length body"
                        , rid
                        , flow
                );
                break;
            }
            
            out.write(buffer, 0, bytesRead);
            out.flush();

            remainingBytes -= bytesRead;
            
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, Relayed {} bytes of body, remaining: {}"
                        , rid
                        , flow
                        , bytesRead
                        , remainingBytes
                );
            }
        }
        
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Content-length body relay completed"
                    , rid
                    , flow
            );
        }
    }
    
    /**
     * Relay content-length based request body
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param request HTTP request
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    private static void relayContentLengthBody(String rid, BodyStream.Flow flow, HttpRequest request, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        Integer contentLength = request.getContentLength();
        if (contentLength == null || contentLength <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, No content length or zero content length"
                        , rid
                        , flow
                );
            }
            return;
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying content-length body: {} bytes"
                    , rid
                    , flow
                    , contentLength
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int remainingBytes = contentLength;
        
        while (remainingBytes > 0) {
            int bytesToRead = Math.min(buffer.length, remainingBytes);
            int bytesRead = in.read(buffer, 0, bytesToRead);
            
            if (bytesRead == -1) {
                logger.warn("{}, {}, Unexpected end of stream while reading content-length body"
                        , rid
                        , flow
                );
                break;
            }
            
            out.write(buffer, 0, bytesRead);
            out.flush();

            remainingBytes -= bytesRead;
            
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, Relayed {} bytes of body, remaining: {}"
                        , rid
                        , flow
                        , bytesRead
                        , remainingBytes
                );
            }
        }
        
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Content-length body relay completed"
                    , rid
                    , flow
            );
        }
    }
    
    /**
     * Relay chunked transfer encoding response body
     * @param rid Request ID for logging
     * @param in Input stream reader
     * @param out Output stream writer
     * @param flow Body stream flow direction (REQ/RES)
     * @throws IOException when I/O error occurs
     */
    private static void relayChunkedBody(String rid, BodyStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying chunked body"
                    , rid
                    , flow
            );
        }

        ByteBuffer chunkSizeBuffer = new ByteBuffer(HTTP.CHUNK_SIZE_LINE_LENGTH);
        byte[] chunkBodyBuffer = new byte[HTTP.BODY_BUFFER_SIZE];
        
        while (true) {
            // Read chunk size line
            int bytesRead = in.readLine(chunkSizeBuffer, HTTP.CHUNK_SIZE_LINE_LENGTH);
            
            if (bytesRead == -1) {
                logger.warn("{}, {}, Unexpected end of stream while reading chunk size"
                        , rid
                        , flow
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
                logger.error("{}, {}, Invalid chunk size: {}"
                        , rid
                        , flow
                        , chunkSizeLine
                );
                break;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, chunk size: {}"
                        , rid
                        , flow
                        , chunkSize
                );
            }

            // Write chunk size to client
            out.write(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            out.write(HTTP.CRLF);
            
            if (chunkSize == 0) {
                // End of chunked body - read and relay trailer headers
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, End of chunked body (chunk size 0) - reading trailer headers"
                            , rid
                            , flow
                    );
                }
                
                out.write(HTTP.CRLF);
                out.flush();
                break;
            }
            
            // Relay chunk data
            int remainingBytes = chunkSize;
            while (remainingBytes > 0) {
                int bytesToRead = Math.min(chunkBodyBuffer.length, remainingBytes);
                bytesRead = in.read(chunkBodyBuffer, 0, bytesToRead);
                
                if (bytesRead == -1) {
                    logger.warn("{}, {}, Unexpected end of stream while reading chunk data"
                            , rid
                            , flow
                    );
                    break;
                }
                out.write(chunkBodyBuffer, 0, bytesRead);
                out.flush();

                remainingBytes -= bytesRead;
                
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Relayed {} bytes of chunk, remaining: {}"
                            , rid
                            , flow
                            , bytesRead
                            , remainingBytes
                    );
                }
            }
            
            // Read and relay chunk end (CR-LF)
            int cr = in.read();
            int lf = in.read();
            if (cr == '\r' && lf == '\n') {
                out.write(HTTP.CRLF);
                out.flush();
            } else {
                logger.warn("{}, {}, Invalid chunk end marker", rid, flow);
                break;
            }
        }
        
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Chunked body relay completed"
                    , rid
                    , flow
            );
        }
    }
    
    /**
     * Relay response body until connection close
     * @param rid Request ID for logging
     * @param in Input stream reader
     * @param out Output stream writer
     * @param flow Body stream flow direction (REQ/RES)
     * @throws IOException when I/O error occurs
     */
    private static void relayUntilCloseBody(String rid, BodyStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying until-close body"
                    , rid
                    , flow
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int totalBytesRelayed = 0;
        
        while (true) {
            int bytesRead = in.read(buffer);
            
            if (bytesRead == -1) {
                // End of stream
                break;
            }
            
            out.write(buffer, 0, bytesRead);
            totalBytesRelayed += bytesRead;
            
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, Relayed {} bytes of body, total: {}"
                        , rid
                        , flow
                        , bytesRead
                        , totalBytesRelayed
                );
            }
        }
        
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Until-close body relay completed, total bytes: {}"
                    , rid
                    , flow
                    , totalBytesRelayed
            );
        }
    }
}
