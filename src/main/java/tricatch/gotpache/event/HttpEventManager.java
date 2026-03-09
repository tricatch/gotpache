package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Manager for HTTP event pipeline
 * 
 * Flow: Request → HttpEvent → EventQueue → WorkerPool → IP Subscriber Map / DropConsumer
 * 
 * - EventQueue: Single shared queue for HttpEvents
 * - WorkerPool: 4~8 threads consuming from queue
 * - IP Subscriber Map: Registered consumers per client IP
 * - DropConsumer: Single instance for unregistered IPs
 */
public class HttpEventManager {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventManager.class);

    private static final int DEFAULT_WORKER_COUNT = 6;
    private static final int MIN_WORKER_COUNT = 4;
    private static final int MAX_WORKER_COUNT = 8;

    private static HttpEventManager instance;

    private final EventQueue eventQueue;
    private final Map<String, HttpEventConsumer> subscriberMap;
    private final List<HttpEventMonitorConsumer> monitorConsumers;
    private final HttpEventDropConsumer httpEventDropConsumer;
    private final ExecutorService workerPool;
    private volatile boolean running = false;

    private HttpEventManager(int workerCount) {
        this.eventQueue = new EventQueue();
        this.subscriberMap = new ConcurrentHashMap<>();
        this.monitorConsumers = new CopyOnWriteArrayList<>();
        this.httpEventDropConsumer = new HttpEventDropConsumer();

        int workers = Math.max(MIN_WORKER_COUNT, Math.min(MAX_WORKER_COUNT, workerCount));
        this.workerPool = Executors.newFixedThreadPool(workers, new ThreadFactory() {
            private int counter = 0;

            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "HttpEventWorker-" + (++counter));
                t.setDaemon(true);
                return t;
            }
        });

        startWorkers(workers);
        logger.info("HttpEventManager initialized: EventQueue → WorkerPool({} threads) → IP Subscriber / DropConsumer", workers);
    }

    private void startWorkers(int count) {
        running = true;
        for (int i = 0; i < count; i++) {
            workerPool.execute(this::workerLoop);
        }
    }

    /**
     * Worker loop: take from EventQueue → dispatch to IP Subscriber or DropConsumer
     */
    private void workerLoop() {
        while (running) {
            try {
                HttpEvent event = eventQueue.take();
                dispatch(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Worker error: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * Dispatch event: registered IP → Subscriber, else → DropConsumer.
     * Also broadcast to all monitor consumers (SSE).
     */
    private void dispatch(HttpEvent event) {
        if (event == null || event.getClientId() == null) {
            logger.warn("Invalid HttpEvent: {}", event);
            return;
        }

        HttpEventConsumer httpEventConsumer = subscriberMap.get(event.getClientId());

        if (httpEventConsumer != null) {
            httpEventConsumer.process(event);
        } else {
            httpEventDropConsumer.process(event);
        }

        // Broadcast to all monitor SSE consumers
        for (HttpEventMonitorConsumer mc : monitorConsumers) {
            try {
                mc.process(event);
            } catch (Exception e) {
                logger.debug("Monitor consumer error (may be disconnected): {}", e.getMessage());
            }
        }
    }

    /**
     * Initialize HttpEventManager with default worker count (6)
     */
    public static synchronized void initialize() {
        initialize(DEFAULT_WORKER_COUNT);
    }

    /**
     * Initialize HttpEventManager with custom worker count (4~8)
     */
    public static synchronized void initialize(int workerCount) {
        if (instance != null) {
            logger.warn("HttpEventManager already initialized");
            return;
        }
        instance = new HttpEventManager(workerCount);
    }

    /**
     * Get singleton instance (lazy init with default)
     */
    public static HttpEventManager getInstance() {
        if (instance == null) {
            synchronized (HttpEventManager.class) {
                if (instance == null) {
                    instance = new HttpEventManager(DEFAULT_WORKER_COUNT);
                    logger.info("HttpEventManager initialized with default worker count");
                }
            }
        }
        return instance;
    }

    /**
     * Enqueue HttpEvent to pipeline
     * Request → HttpEvent 생성 → EventQueue → WorkerPool → IP Subscriber / DropConsumer
     */
    public void enqueue(HttpEvent event) {
        if (event == null || event.getClientId() == null) {
            logger.warn("Invalid HttpEvent: {}", event);
            return;
        }

        if (!eventQueue.offer(event)) {
            logger.warn("EventQueue full, dropping event: clientId={}, rid={}", event.getClientId(), event.getRid());
        }
    }

    /**
     * Add subscriber for IP (clientId)
     */
    public void add(String clientId, HttpEventConsumer consumer) {
        if (clientId == null || consumer == null) {
            throw new IllegalArgumentException("clientId and consumer cannot be null");
        }
        if (!clientId.equals(consumer.getClientId())) {
            throw new IllegalArgumentException(
                    "ClientId mismatch: expected " + clientId + ", got " + consumer.getClientId());
        }
        subscriberMap.put(clientId, consumer);
        logger.debug("Added subscriber for IP: {}", clientId);
    }

    /**
     * Get subscriber for IP
     */
    public HttpEventConsumer getConsumer(String clientId) {
        return subscriberMap.get(clientId);
    }

    /**
     * Check if subscriber exists for IP
     */
    public boolean hasConsumer(String clientId) {
        return subscriberMap.containsKey(clientId);
    }

    /**
     * Add monitor consumer (SSE). Receives all events regardless of clientId.
     */
    public void addMonitorConsumer(HttpEventMonitorConsumer consumer) {
        if (consumer == null) {
            throw new IllegalArgumentException("monitor consumer cannot be null");
        }
        monitorConsumers.add(consumer);
        logger.debug("Added monitor consumer: {}", consumer.getClientId());
    }

    /**
     * Remove monitor consumer
     */
    public void removeMonitorConsumer(String clientId) {
        monitorConsumers.removeIf(mc -> clientId.equals(mc.getClientId()));
        logger.debug("Removed monitor consumer: {}", clientId);
    }

    /**
     * Shutdown pipeline
     */
    public void shutdown() {
        logger.info("Shutting down HttpEventManager");
        running = false;

        workerPool.shutdown();
        try {
            if (!workerPool.awaitTermination(10, TimeUnit.SECONDS)) {
                workerPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            workerPool.shutdownNow();
            Thread.currentThread().interrupt();
        }

        subscriberMap.clear();
        monitorConsumers.clear();
    }
}
