package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.BodyStream;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpResponse;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;

import java.io.IOException;

/**
 * Class for handling content-length based HTTP body relay operations
 */
public class RelayContentLength {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayContentLength.class);
    
    /**
     * Relay content-length based response body
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param response HTTP response
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    public static void relay(String rid, BodyStream.Flow flow, HttpResponse response, HttpStreamReader in, HttpStreamWriter out) throws IOException {
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
    public static void relay(String rid, BodyStream.Flow flow, HttpRequest request, HttpStreamReader in, HttpStreamWriter out) throws IOException {
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
}
