package tricatch.gotpache.event;

import java.io.IOException;

/**
 * Interface for HTTP event consumer
 * Each clientId can have its own consumer implementation
 */
public interface HttpEventConsumer {
    
    /**
     * Get clientId that this consumer handles
     * @return clientId
     */
    String getClientId();

    /**
     * Get channelID that this consumer handles
     * @return channelId
     */
    String getChannelId();
    

    /**
     * Process an HTTP event directly (synchronous)
     * @param event HTTP event to process
     */
    void process(HttpEvent event) throws IOException;
}
