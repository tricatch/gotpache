package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.BodyStream;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;

import java.io.IOException;

/**
 * Class for handling until-close HTTP body relay operations
 */
public class RelayUntilClose {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayUntilClose.class);
    
    /**
     * Relay response body until connection close
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param in Input stream reader
     * @param out Output stream writer
     * @throws IOException when I/O error occurs
     */
    public static void relay(String rid, BodyStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
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
