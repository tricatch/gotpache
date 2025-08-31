package tricatch.gotpache.util;

import tricatch.gotpache.http.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class HtmlUtil {

    public static byte[] toBytes(List<String> list) throws IOException {

        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

        for (String line: list){

            buf.write( line.getBytes(StandardCharsets.UTF_8) );
            buf.write( HTTP.CRLF );
        }

        return buf.toByteArray();
    }
}
