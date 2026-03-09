package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
    private final Map<String, HttpEventConsumer> ipSubscriberMap;
    private final HttpEventDropConsumer httpEventDropConsumer;
    private final ExecutorService workerPool;
    private volatile boolean running = false;

    private HttpEventManager(int workerCount) {
        this.eventQueue = new EventQueue();
        this.ipSubscriberMap = new ConcurrentHashMap<>();
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
     * Dispatch event: registered IP → Subscriber, else → DropConsumer
     */
    private void dispatch(HttpEvent event) {
        if (event == null || event.getClientId() == null) {
            logger.warn("Invalid HttpEvent: {}", event);
            return;
        }

        HttpEventConsumer httpEventConsumer = ipSubscriberMap.get(event.getClientId());

        if (httpEventConsumer != null) {
            httpEventConsumer.process(event);
        } else {
            httpEventDropConsumer.process(event);
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
     * Get subscriber for IP
     */
    public HttpEventConsumer getConsumer(String clientId) {
        return ipSubscriberMap.get(clientId);
    }

    /**
     * Check if subscriber exists for IP
     */
    public boolean hasConsumer(String clientId) {
        return ipSubscriberMap.containsKey(clientId);
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

        ipSubscriberMap.clear();
    }
}
