package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.io.HttpStream;
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
     * @return HttpStream.Connection indicating whether connection should be closed
     * @throws IOException when I/O error occurs
     */
    public static HttpStream.Connection relayResponseBody(String rid, HttpStream.Flow flow, HttpResponse response, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        HttpStream httpStream = response.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying body with type: {}"
                    , rid
                    , flow
                    , httpStream
            );
        }
        
        switch (httpStream) {
            case NONE:
            case NULL:
                // No-body to relay
                if (logger.isDebugEnabled()) {
                    logger.trace("{}, {}, No body to relay"
                            , rid
                            , flow
                    );
                }
                return HttpStream.Connection.KEEP_ALIVE;
                
            case CONTENT_LENGTH:
                return RelayContentLength.relay(rid, flow, response, in, out);
                
            case CHUNKED:
                return RelayChunked.relay(rid, flow, in, out);
                
            case WEBSOCKET:
                return RelayWebSocket.relay(rid, flow, in, out);
                
            case UNTIL_CLOSE:
                return RelayUntilClose.relay(rid, flow, in, out);
                
            default:
                logger.warn("{}, {}, Unknown body stream type: {}"
                        , rid
                        , flow
                        , httpStream
                );
                return HttpStream.Connection.KEEP_ALIVE;
        }
    }
    
    /**
     * Relay request body to server based on body stream type
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param request HTTP request containing body stream information
     * @param in Input stream reader
     * @param out Output stream writer
     * @return HttpStream.Connection indicating whether connection should be closed
     * @throws IOException when I/O error occurs
     */
    public static HttpStream.Connection relayRequestBody(String rid, HttpStream.Flow flow, HttpRequest request, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        HttpStream httpStream = request.getBodyStream();
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying body with type: {}"
                    , rid
                    , flow
                    , httpStream
            );
        }
        
        switch (httpStream) {
            case NONE:
            case NULL:
                // No-body to relay
                if (logger.isDebugEnabled()) {
                    logger.trace("{}, {}, No body to relay"
                            , rid
                            , flow
                    );
                }
                return HttpStream.Connection.KEEP_ALIVE;
                
            case CONTENT_LENGTH:
                return RelayContentLength.relay(rid, flow, request, in, out);
                
            case CHUNKED:
                return RelayChunked.relay(rid, flow, in, out);
                
            case WEBSOCKET:
                return RelayWebSocket.relay(rid, flow, in, out);
                
            case UNTIL_CLOSE:
                return RelayUntilClose.relay(rid, flow, in, out);
                
            default:
                logger.warn("{}, {}, Unknown body stream type: {}"
                        , rid
                        , flow
                        , httpStream
                );
                return HttpStream.Connection.KEEP_ALIVE;
        }
    }
    

    

    

    

}
