package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.pass.Stopable;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;

public class VThreadExecutor {

    private static final Logger logger = LoggerFactory.getLogger(VThreadExecutor.class);

    private static final AtomicLong threadCounter = new AtomicLong(0);

    private static final Map<String, Stopable> runningExecutors  = new ConcurrentHashMap<>();

    public static Thread run(Stopable stopable) {

        long threadNumber = threadCounter.incrementAndGet();

        Thread thread = Thread.ofVirtual().name("vt-pass-" + threadNumber +"x0").start(stopable);

        runningExecutors.put(thread.getName(), stopable);

        return thread;
    }

    public static Thread run(Stopable stopable, String name) {

        Thread thread = Thread.ofVirtual().name(name).start(stopable);

        runningExecutors.put(thread.getName(), stopable);

        return thread;
    }

    public static void removeVirtualThread(Thread thread){
        runningExecutors.remove(thread.getName());
    }

    public static void stopAll(){

        try {
            Set<String> names = runningExecutors.keySet();

            for (String name : names) {

                Stopable stopable = runningExecutors.get(name);

                if (stopable == null) continue;

                if (logger.isDebugEnabled()) logger.debug("stopped running vt - {}", stopable.getName());

                stopable.stop();
            }

        } catch (Exception e){
            logger.error("errorVtStopAll - " + e.getMessage(), e);
        }

    }

    public static Executor getExecutor() {
        return runnable -> Thread.ofVirtual().start(runnable);
    }
}
