package tricatch.gotpache.util;

import java.nio.charset.Charset;

public class StringUtils {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static String toString(byte[] buf){

        if( buf==null ) return null;

        return new String(buf, UTF8);
    }

    public static String trim(byte[] buf){

        String str = toString(buf);

        if( str!=null ) str = str.trim();

        return str;
    }
    
    public static boolean isEmpty(String input) {
    	
    	if( input==null ) return true;
    	if( input.length()==0 ) return true;
    	if( input.trim().length()==0 ) return true;
    	
    	return false;
    }
}
