package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Abstract base class for HttpEventConsumer implementations
 * Provides common functionality for event queue management and processing
 */
public abstract class AbstractHttpEventConsumer implements HttpEventConsumer {
    
    private static final Logger logger = LoggerFactory.getLogger(AbstractHttpEventConsumer.class);
    
    protected final String clientId;
    protected final BlockingQueue<HttpEvent> queue;
    protected final Map<String, RequestResponsePair> pendingPairs;
    protected ExecutorService consumerThread;
    protected volatile boolean running = false;
    
    public AbstractHttpEventConsumer(String clientId) {
        this.clientId = clientId;
        this.queue = new LinkedBlockingQueue<>();
        this.pendingPairs = new ConcurrentHashMap<>();
    }
    
    @Override
    public String getClientId() {
        return clientId;
    }
    
    @Override
    public void start() {
        if (running) {
            logger.warn("Consumer for clientId {} is already running", clientId);
            return;
        }
        running = true;
        consumerThread = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "HttpEventConsumer-" + clientId);
            t.setDaemon(true);
            return t;
        });
        consumerThread.execute(this::processEvents);
        logger.debug("Started HTTP event consumer for clientId: {}", clientId);
    }
    
    @Override
    public void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (consumerThread != null) {
            consumerThread.shutdown();
            try {
                if (!consumerThread.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    consumerThread.shutdownNow();
                }
            } catch (InterruptedException e) {
                consumerThread.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.debug("Stopped HTTP event consumer for clientId: {}", clientId);
    }
    
    @Override
    public void enqueue(HttpEvent event) {
        if (!clientId.equals(event.getClientId())) {
            throw new IllegalArgumentException(
                "ClientId mismatch: expected " + clientId + ", got " + event.getClientId());
        }
        if (!queue.offer(event)) {
            logger.warn("Failed to enqueue HTTP event for clientId: {}, rid: {}", clientId, event.getRid());
        }
    }
    
    @Override
    public boolean isRunning() {
        return running;
    }
    
    /**
     * Process events from queue
     * Subclasses can override this for custom processing
     */
    protected void processEvents() {
        while (running) {
            HttpEvent event = null;
            try {
                event = queue.take();
                processEvent(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error processing HTTP event for clientId: {}, rid: {}", 
                    clientId, event != null ? event.getRid() : "unknown", e);
            }
        }
        logger.debug("Event processing loop ended for clientId: {}", clientId);
    }
    
    /**
     * Process a single event
     * Subclasses must implement this
     */
    protected abstract void processEvent(HttpEvent event);
    
    /**
     * Write log when REQ/RES pair is complete
     * Subclasses must implement this
     */
    protected abstract void writeLog(RequestResponsePair pair);
}
