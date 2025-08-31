package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.exception.MaxBufferExceedException;
import tricatch.gotpache.http.HTTP;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * BufferedInputStream extension that provides line reading functionality
 */
public class HttpStreamReader extends BufferedInputStream {

    //private static final Logger logger = LoggerFactory.getLogger(HttpStreamReader.class);

    /**
     * Creates HttpStreamReader with specified buffer size
     * @param in input stream
     * @param size buffer size
     */
    public HttpStreamReader(InputStream in, int size) {
        super(in, size);
    }

    /**
     * Reads a line up to the maximum length and stores it in the provided byte array
     * Recognizes CRLF(\r\n) or LF(\n) as line terminators
     * Expands buffer by 2x when line terminator is not found, but does not exceed max
     * 
     * @param buffer byte array to store the line
     * @param max maximum number of bytes to read
     * @return actual number of bytes read, or -1 if end of stream is reached
     * @throws IOException when I/O error occurs
     */
    public int readLine(ByteBuffer buffer, int max) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("Buffer cannot be null");
        }
        if (max < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }
        if (buffer.getBuffer().length > max) {
            throw new IllegalArgumentException("Buffer length cannot exceed max (" + buffer.getBuffer().length + ", " + max + ")" );
        }

        int bytesRead = 0;
        int ch;
        boolean foundCR = false;

        while (bytesRead < max) {
            ch = read();
            
            if (ch == -1) {
                // End of stream reached
                if (bytesRead > 0) {
                    buffer.setLength(bytesRead);
                    return bytesRead;
                }
                return -1;
            }

            if (ch == '\r') {
                foundCR = true;
                continue;
            }

            if (ch == '\n') {
                // Found CRLF or LF, so end of line
                buffer.setLength(bytesRead);
                return bytesRead;
            }

            if (foundCR) {
                // CR followed by non-LF character, add CR to buffer
                if (bytesRead >= buffer.getBuffer().length) {
                    // Buffer is full, expand it
                    expandBuffer(buffer, max);
                }
                buffer.getBuffer()[bytesRead++] = (byte) '\r';
                foundCR = false;
            }

            if (bytesRead >= buffer.getBuffer().length) {
                // Buffer is full, expand it
                expandBuffer(buffer, max);
            }
            buffer.getBuffer()[bytesRead++] = (byte) ch;
        }

        // Reached maximum length without finding line terminator
        buffer.setLength(bytesRead);
        throw new IOException("Maximum line length (" + max + ") exceeded without finding line terminator");
    }
    
    /**
     * Reads all HTTP headers and populates the provided HeaderLines object
     * Stops reading when an empty line (CRLF only) is encountered
     * 
     * @param headers HeaderLines object to populate with headers
     * @param maxLineLength maximum length for each header line
     * @return total number of bytes read, or -1 if end of stream is reached
     * @throws IOException when I/O error occurs
     */
    public int readHeaders(HeaderLines headers, int maxLineLength) throws IOException {
        if (headers == null) {
            throw new NullPointerException("Headers cannot be null");
        }
        if (maxLineLength <= 0) {
            throw new IllegalArgumentException("Max line length must be positive");
        }
        
        headers.clear(); // Clear existing headers
        int totalBytesRead = 0;
        
        
        while (true) {
            ByteBuffer lineBuffer = new ByteBuffer(HTTP.INIT_HEADER_LENGTH);
            int bytesRead = readLine(lineBuffer, maxLineLength);

            if (bytesRead == -1) {
                // End of stream reached
                return totalBytesRead > 0 ? totalBytesRead : -1;
            }

            totalBytesRead += bytesRead;
            totalBytesRead += 2; // Add CRLF bytes
            
            // Check if this is an empty line (end of headers)
            if (bytesRead == 0) {
                break;
            }
            
            // Add header line directly to the list
            headers.add(lineBuffer);
        }
        
        return totalBytesRead;
    }


    /**
     * Expands buffer by 2x but does not exceed max
     * 
     * @param oldBuffer existing buffer
     * @param max maximum allowed size
     * @return expanded buffer
     */
    private void expandBuffer(ByteBuffer oldBuffer, int max) throws MaxBufferExceedException {
        int newSize = Math.min(oldBuffer.getBuffer().length * 2, max);
        if (newSize <= oldBuffer.getBuffer().length) {
            // Cannot expand further
            throw new MaxBufferExceedException("expand buffer error - max=" + max);
        }
        
        byte[] newBuffer = new byte[newSize];

        System.arraycopy(oldBuffer.getBuffer(), 0, newBuffer, 0, oldBuffer.getBuffer().length);

        oldBuffer.setBuffer(newBuffer);
    }


}
