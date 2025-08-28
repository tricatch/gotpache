package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;

public class HttpStreamWriter {

    private static Logger logger = LoggerFactory.getLogger(HttpStreamWriter.class);

    private OutputStream out;

    public HttpStreamWriter(OutputStream out) {
        this.out = out;
    }


    public void close() throws IOException {
        this.out.close();
    }

    public void writeHeaders(HeaderLines headerLines) throws IOException {
        if (headerLines == null) {
            throw new NullPointerException("HeaderLines cannot be null");
        }
        
        // Write all header lines
        for (ByteBuffer headerBuffer : headerLines) {
            if (logger.isTraceEnabled()){
                logger.trace( "{}", new String(headerBuffer.getBuffer(), 0, headerBuffer.getLength()));
            }
            out.write(headerBuffer.getBuffer(), 0, headerBuffer.getLength());
            out.write('\r');
            out.write('\n');
        }
        
        // Write empty line to end headers
        out.write('\r');
        out.write('\n');
        out.flush();
    }

}
