package tricatch.gotpache.event;

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
     * Start consuming HTTP events
     * Called when consumer is registered
     */
    void start();
    
    /**
     * Stop consuming HTTP events
     * Called when consumer is unregistered
     */
    void stop();
    
    /**
     * Enqueue an HTTP event for processing
     * @param event HTTP event to process
     */
    void enqueue(HttpEvent event);
    
    /**
     * Check if consumer is running
     * @return true if running, false otherwise
     */
    boolean isRunning();
}
