package tricatch.gotpache.server;

import java.util.concurrent.atomic.AtomicLong;

public class VThreadExecutor {
    
    private static final AtomicLong threadCounter = new AtomicLong(0);

    public static Thread run(Runnable runnable) {
        long threadNumber = threadCounter.incrementAndGet();
        return Thread.ofVirtual().name("vt-pass-" + threadNumber).start(runnable);
    }

    public static Thread run(Runnable runnable, String name) {
        long threadNumber = threadCounter.incrementAndGet();
        return Thread.ofVirtual().name(name).start(runnable);
    }
}
