package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for HTTP event consumers
 * Handles registration, unregistration, and event routing
 */
public class HttpEventManager {
    
    private static final Logger logger = LoggerFactory.getLogger(HttpEventManager.class);
    
    private static HttpEventManager instance;
    private final Map<String, HttpEventConsumer> clientConsumers;
    private final HttpEventConsumerFactory consumerFactory;
    
    private HttpEventManager(HttpEventConsumerFactory factory) {
        this.clientConsumers = new ConcurrentHashMap<>();
        this.consumerFactory = factory;
    }
    
    /**
     * Initialize HttpEventManager with factory
     */
    public static synchronized void initialize(HttpEventConsumerFactory factory) {
        if (instance != null) {
            logger.warn("HttpEventManager already initialized");
            return;
        }
        instance = new HttpEventManager(factory);
        logger.info("HttpEventManager initialized");
    }
    
    /**
     * Get singleton instance
     */
    public static HttpEventManager getInstance() {
        if (instance == null) {
            synchronized (HttpEventManager.class) {
                if (instance == null) {
                    instance = new HttpEventManager(new DefaultHttpEventConsumerFactory());
                    logger.info("HttpEventManager initialized with default factory");
                }
            }
        }
        return instance;
    }
    
    /**
     * Enqueue HTTP event
     * If no consumer is registered for clientId, use DropConsumer to drop the event
     * All events are queued and processed by consumer threads
     */
    public void enqueue(HttpEvent event) {
        if (event == null || event.getClientId() == null) {
            logger.warn("Invalid HTTP event: {}", event);
            return;
        }
        
        HttpEventConsumer consumer = clientConsumers.get(event.getClientId());
        
        if (consumer == null) {
            // No consumer registered, get or create DropConsumer
            consumer = clientConsumers.computeIfAbsent(
                event.getClientId(),
                k -> {
                    DropHttpEventConsumer dropConsumer = new DropHttpEventConsumer(k);
                    dropConsumer.start();
                    logger.debug("Created and started drop consumer for clientId: {}", k);
                    return dropConsumer;
                }
            );
        }
        
        // Enqueue event to consumer's queue (will be processed by consumer thread)
        consumer.enqueue(event);
    }
    
    /**
     * Register consumer for specific clientId
     */
    public void registerConsumer(String clientId, HttpEventConsumer consumer) {
        if (clientId == null || consumer == null) {
            throw new IllegalArgumentException("clientId and consumer cannot be null");
        }
        
        if (!clientId.equals(consumer.getClientId())) {
            throw new IllegalArgumentException(
                "ClientId mismatch: expected " + clientId + ", got " + consumer.getClientId());
        }
        
        HttpEventConsumer existing = clientConsumers.putIfAbsent(clientId, consumer);
        if (existing != null) {
            throw new IllegalStateException("Consumer already exists for clientId: " + clientId);
        }
        consumer.start();
        logger.debug("Registered consumer for clientId: {}", clientId);
    }
    
    /**
     * Unregister consumer for specific clientId
     */
    public void unregisterConsumer(String clientId) {
        if (clientId == null) {
            return;
        }
        
        HttpEventConsumer consumer = clientConsumers.remove(clientId);
        if (consumer != null) {
            consumer.stop();
            logger.debug("Unregistered consumer for clientId: {}", clientId);
        }
    }
    
    /**
     * Get consumer for clientId
     */
    public HttpEventConsumer getConsumer(String clientId) {
        return clientConsumers.get(clientId);
    }
    
    /**
     * Check if consumer exists for clientId
     */
    public boolean hasConsumer(String clientId) {
        return clientConsumers.containsKey(clientId);
    }
    
    /**
     * Shutdown all consumers
     */
    public void shutdown() {
        logger.info("Shutting down all HTTP event consumers");
        for (Map.Entry<String, HttpEventConsumer> entry : clientConsumers.entrySet()) {
            try {
                entry.getValue().stop();
            } catch (Exception e) {
                logger.error("Error stopping consumer for clientId: {}", entry.getKey(), e);
            }
        }
        clientConsumers.clear();
    }
}
