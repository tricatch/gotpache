package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpStream;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;
import tricatch.gotpache.event.HttpEvent;
import tricatch.gotpache.event.HttpEventManager;
import tricatch.gotpache.event.HttpEventType;

import java.io.IOException;

/**
 * Class for handling until-close HTTP body relay operations
 */
public class RelayUntilClose {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayUntilClose.class);
    
    /**
     * Relay response body until connection close
     * @param clientId Client identifier
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param in Input stream reader
     * @param out Output stream writer
     * @return HttpStream.Connection indicating whether connection should be closed
     * @throws IOException when I/O error occurs
     */
    public static HttpStream.Connection relay(String clientId, String rid, HttpStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying until-close body"
                    , rid
                    , flow
            );
        }
        
        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int totalBytesRelayed = 0;
        java.io.ByteArrayOutputStream bodyCollector = new java.io.ByteArrayOutputStream();
        
        while (true) {
            int bytesRead = in.read(buffer);
            
            if (bytesRead == -1) {
                // End of stream
                break;
            }
            
            out.write(buffer, 0, bytesRead);
            totalBytesRelayed += bytesRead;
            
            // Collect body data for logging
            bodyCollector.write(buffer, 0, bytesRead);
            
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
        
        // Enqueue body HttpEvent
        HttpEvent bodyEvent = new HttpEvent(clientId, rid, 
            flow == HttpStream.Flow.REQ ? HttpEventType.REQ_BODY : HttpEventType.RES_BODY);
        bodyEvent.setBody(bodyCollector.toByteArray());
        bodyEvent.setHttpStream(HttpStream.UNTIL_CLOSE);
        HttpEventManager.getInstance().enqueue(bodyEvent);

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Until-close body relay completed, total bytes: {}"
                    , rid
                    , flow
                    , totalBytesRelayed
            );
        }
        
        // Until-close means connection should be closed
        return HttpStream.Connection.CLOSE;
    }
}
