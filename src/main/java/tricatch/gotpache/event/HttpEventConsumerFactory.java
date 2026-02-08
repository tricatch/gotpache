package tricatch.gotpache.event;

/**
 * Factory interface for creating HttpEventConsumer instances
 */
public interface HttpEventConsumerFactory {
    
    /**
     * Create HttpEventConsumer for given clientId
     * @param clientId client identifier
     * @return HttpEventConsumer instance
     */
    HttpEventConsumer create(String clientId);
}
