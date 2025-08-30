package tricatch.gotpache.server;

import java.util.concurrent.atomic.AtomicLong;

public class VThreadExecutor {
    
    private static final AtomicLong threadCounter = new AtomicLong(0);

    public static void run(Runnable runnable) {
        long threadNumber = threadCounter.incrementAndGet();
        Thread.ofVirtual().name("vt-pass-" + threadNumber).start(runnable);
    }
}
