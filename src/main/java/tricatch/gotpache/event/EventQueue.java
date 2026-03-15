package tricatch.gotpache.event;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Single shared queue for HttpEvents
 * Request → HttpEvent → EventQueue → WorkerPool
 */
public class EventQueue {

    private static final int DEFAULT_CAPACITY = 1000;

    private final BlockingQueue<HttpEvent> queue;

    public EventQueue() {
        this(DEFAULT_CAPACITY);
    }

    public EventQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Enqueue HttpEvent (non-blocking)
     * @return true if offered successfully, false if queue full
     */
    public boolean offer(HttpEvent event) {
        return queue.offer(event);
    }

    /**
     * Take HttpEvent (blocking)
     */
    public HttpEvent take() throws InterruptedException {
        return queue.take();
    }

    /**
     * Get current queue size
     */
    public int size() {
        return queue.size();
    }
}
