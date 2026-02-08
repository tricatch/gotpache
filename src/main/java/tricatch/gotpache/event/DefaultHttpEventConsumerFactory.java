package tricatch.gotpache.event;

/**
 * Default factory implementation
 * Creates DropHttpEventConsumer by default
 */
public class DefaultHttpEventConsumerFactory implements HttpEventConsumerFactory {
    
    @Override
    public HttpEventConsumer create(String clientId) {
        return new DropHttpEventConsumer(clientId);
    }
}
