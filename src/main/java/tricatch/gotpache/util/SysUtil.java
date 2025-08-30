package tricatch.gotpache.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

public class SysUtil {

	private static final AtomicLong lastTime = new AtomicLong(0);
	
	public static String getHostname() {
		
		String hostname = null;
		
		try {
			
		    hostname = InetAddress.getLocalHost().getHostName();
		    
		    if( !StringUtils.isEmpty(hostname) ) return hostname;

		} catch (UnknownHostException e) {
		    //next
		}

		hostname = System.getenv("COMPUTERNAME");
		if( !StringUtils.isEmpty(hostname) ) return hostname;
		
		hostname = System.getenv("HOSTNAME");
		if( !StringUtils.isEmpty(hostname) ) return hostname;
		
		return "Unknown-Host";
	}


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
