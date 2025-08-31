package tricatch.gotpache.util;

import java.util.concurrent.atomic.AtomicLong;

public class SysUtil {

	private static final AtomicLong lastTime = new AtomicLong(0);

	public static String generateRequestId() {

		long now = System.currentTimeMillis();
		long last;
		do {
			last = lastTime.get();
			if (now <= last) {
				now = last + 1;
			}
		} while (!lastTime.compareAndSet(last, now));

		return String.valueOf(now);
	}
}
