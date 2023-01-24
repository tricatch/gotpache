package tricatch.gotpache.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SysUtil {
	
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

}
