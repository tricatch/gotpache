package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;

public class HttpBufferedWriter {

    private static Logger logger = LoggerFactory.getLogger(HttpBufferedWriter.class);

    private StreamMode streamMode = null;
    private OutputStream out = null;
    public HttpBufferedWriter(StreamMode streamMode, OutputStream out){
        this.streamMode = streamMode;
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void flush() throws IOException {
        out.flush();
    }

    public void write(byte[] buf, int start, int len) throws IOException {

        if( logger.isTraceEnabled() ){
            String raw = ByteUtils.toHexPretty(buf, start, start + len);
            logger.trace( "{} write - raw( {} - {} ), length={}\n{}"
                    , this.streamMode
                    , start
                    , start + len
                    , len
                    , raw
            );
        }

        out.write(buf, start, len);
    }
}
