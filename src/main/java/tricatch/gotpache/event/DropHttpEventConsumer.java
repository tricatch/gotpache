package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop HTTP event consumer implementation
 * Drops all HTTP events without processing
 * Used when no consumer is registered for a clientId
 * Processes events from queue in a separate thread
 */
public class DropHttpEventConsumer extends AbstractHttpEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(DropHttpEventConsumer.class);
    
    public DropHttpEventConsumer(String clientId) {
        super(clientId);
    }
    
    @Override
    protected void processEvent(HttpEvent event) {
        // Drop the event and print full log as a single log entry
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("=== DROPPED HTTP Event (No Consumer Registered) ===\n");
        logBuilder.append("ClientId: ").append(event.getClientId()).append("\n");
        logBuilder.append("Rid: ").append(event.getRid()).append("\n");
        logBuilder.append("Type: ").append(event.getType()).append("\n");
        logBuilder.append("Timestamp: ").append(event.getTimestamp()).append("\n");
        
        if (event.getHttpStream() != null) {
            logBuilder.append("HttpStream: ").append(event.getHttpStream()).append("\n");
        }
        
        if (event.getHeaders() != null) {
            logBuilder.append("Headers:\n").append(event.getHeaders()).append("\n");
        }
        
        if (event.getBody() != null) {
            int bodySize = event.getBody().length;
            logBuilder.append("Body Size: ").append(bodySize).append(" bytes\n");
            if (bodySize > 0) {
                // Print body content (limit to first 1024 bytes for readability)
                int printLength = Math.min(bodySize, 1024);
                StringBuilder bodyHex = new StringBuilder();
                for (int i = 0; i < printLength; i++) {
                    bodyHex.append(String.format("%02X ", event.getBody()[i]));
                    if ((i + 1) % 16 == 0) {
                        bodyHex.append("\n");
                    }
                }
                if (bodySize > printLength) {
                    bodyHex.append("\n... (truncated, total ").append(bodySize).append(" bytes)");
                }
                logBuilder.append("Body Content (hex):\n").append(bodyHex.toString()).append("\n");
                
                // Try to print as string if it's text
                try {
                    String bodyText = new String(event.getBody(), 0, printLength, "UTF-8");
                    if (isPrintable(bodyText)) {
                        logBuilder.append("Body Content (text):\n").append(bodyText).append("\n");
                    }
                } catch (Exception e) {
                    // Ignore encoding errors
                }
            } else {
                logBuilder.append("Body: (empty)\n");
            }
        } else {
            logBuilder.append("Body: (null)\n");
        }
        
        logBuilder.append("=== End of DROPPED HTTP Event ===");
        
        //logger.info(logBuilder.toString());
    }
    
    @Override
    protected void writeLog(RequestResponsePair pair) {
        // Not used in DropConsumer - events are dropped individually
        // This method should never be called
        logger.warn("writeLog() called on DropConsumer - this should not happen");
    }
    
    /**
     * Check if string contains only printable characters
     */
    private boolean isPrintable(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (char c : str.toCharArray()) {
            if (!Character.isISOControl(c) || c == '\n' || c == '\r' || c == '\t') {
                continue;
            }
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c < 32 || c > 126) {
                return false;
            }
        }
        return true;
    }
}
