package tricatch.gotpache.http.io;

import tricatch.gotpache.http.HTTP;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class HttpStreamWriter extends BufferedOutputStream {

    public HttpStreamWriter(OutputStream out) {
        super(out);
    }

    public void writeHeaders(HeaderLines headerLines) throws IOException {
        if (headerLines == null) {
            throw new NullPointerException("HeaderLines cannot be null");
        }
        
        // Write all header lines
        for (ByteBuffer headerBuffer : headerLines) {
            write(headerBuffer.getBuffer(), 0, headerBuffer.getLength());
            write(HTTP.CRLF);
        }
        
        // Write empty line to end headers
        write(HTTP.CRLF);
        flush();
    }

}
