package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;

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
            out.write(HTTP.CRLF);
        }
        
        // Write empty line to end headers
        out.write(HTTP.CRLF);
        out.flush();
    }

    /**
     * Write byte array to output stream
     * @param data byte array to write
     * @param offset starting offset in the array
     * @param length number of bytes to write
     * @throws IOException when I/O error occurs
     */
    public void write(byte[] data, int offset, int length) throws IOException {
        out.write(data, offset, length);
    }

    /**
     * Write byte array to output stream
     * @param data byte array to write
     * @throws IOException when I/O error occurs
     */
    public void write(byte[] data) throws IOException {
        out.write(data);
    }
    
    /**
     * Write single byte to output stream
     * @param b byte to write
     * @throws IOException when I/O error occurs
     */
    public void write(int b) throws IOException {
        out.write(b);
    }
    
    /**
     * Write single character to output stream
     * @param c character to write
     * @throws IOException when I/O error occurs
     */
    public void write(char c) throws IOException {
        out.write(c);
    }
    
    /**
     * Flush the output stream
     * @throws IOException when I/O error occurs
     */
    public void flush() throws IOException {
        out.flush();
    }

}
