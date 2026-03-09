package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop consumer for HttpEvents when no IP subscriber is registered.
 * Processes events synchronously (called directly from WorkerPool).
 */
public class HttpEventDropConsumer implements HttpEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventDropConsumer.class);

    @Override
    public String getClientId() {
        return "httpEventDropConsumer";
    }

    /**
     * Process and drop the event (no subscriber registered for this IP)
     */
    public void process(HttpEvent event) {
        if (event == null) {
            return;
        }
        // Drop the event - optionally log for debugging
        if (logger.isDebugEnabled()) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("=== DROPPED HttpEvent (No Subscriber) ===\n");
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
                logBuilder.append("Body Size: ").append(event.getBody().length).append(" bytes\n");
            }
            logBuilder.append("=== End of DROPPED HttpEvent ===");
            logger.debug(logBuilder.toString());
        }
    }
}
