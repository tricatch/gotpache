package tricatch.gotpache.util;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class StringUtils {

    private static final Charset UTF8 = StandardCharsets.UTF_8;

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
    	if( input.isEmpty() ) return true;
        return input.trim().isEmpty();
    }
}
