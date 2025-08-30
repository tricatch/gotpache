package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.io.BodyStream;
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
                RelayContentLength.relay(rid, flow, response, in, out);
                break;
                
            case CHUNKED:
                RelayChunked.relay(rid, flow, in, out);
                break;
                
            case WEBSOCKET:
                RelayWebSocket.relay(rid, flow, in, out);
                break;
                
            case UNTIL_CLOSE:
                RelayUntilClose.relay(rid, flow, in, out);
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
                RelayContentLength.relay(rid, flow, request, in, out);
                break;
                
            case CHUNKED:
                RelayChunked.relay(rid, flow, in, out);
                break;
                
            case WEBSOCKET:
                RelayWebSocket.relay(rid, flow, in, out);
                break;
                
            case UNTIL_CLOSE:
                RelayUntilClose.relay(rid, flow, in, out);
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
    

    

    

    

}
