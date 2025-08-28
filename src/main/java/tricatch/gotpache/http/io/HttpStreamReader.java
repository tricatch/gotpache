package tricatch.gotpache.http.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * BufferedInputStream extension that provides line reading functionality
 */
public class HttpStreamReader extends BufferedInputStream {

    /**
     * Creates HttpStreamReader with default buffer size
     * @param in input stream
     */
    public HttpStreamReader(InputStream in) {
        super(in);
    }

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
    public int readLine(byte[] buffer, int max) throws IOException {
        if (buffer == null) {
            throw new NullPointerException("Buffer cannot be null");
        }
        if (max < 0) {
            throw new IllegalArgumentException("Max length cannot be negative");
        }
        if (max > buffer.length) {
            throw new IllegalArgumentException("Max length cannot exceed buffer length");
        }

        int bytesRead = 0;
        int ch;
        boolean foundCR = false;
        byte[] currentBuffer = buffer;
        int currentMax = max;

        while (bytesRead < currentMax) {
            ch = read();
            
            if (ch == -1) {
                // End of stream reached
                return bytesRead > 0 ? bytesRead : -1;
            }

            if (ch == '\r') {
                foundCR = true;
                continue;
            }

            if (ch == '\n') {
                // Found CRLF or LF, so end of line
                return bytesRead;
            }

            if (foundCR) {
                // CR followed by non-LF character, add CR to buffer
                if (bytesRead >= currentBuffer.length) {
                    // Buffer is full, expand it
                    currentBuffer = expandBuffer(currentBuffer, currentMax);
                }
                currentBuffer[bytesRead++] = (byte) '\r';
                foundCR = false;
            }

            if (bytesRead >= currentBuffer.length) {
                // Buffer is full, expand it
                currentBuffer = expandBuffer(currentBuffer, currentMax);
            }
            currentBuffer[bytesRead++] = (byte) ch;
        }

        // Reached maximum length without finding line terminator
        throw new IOException("Maximum line length (" + max + ") exceeded without finding line terminator");
    }
    
    /**
     * Reads a line and stores it in the provided ByteBuffer
     * Recognizes CRLF(\r\n) or LF(\n) as line terminators
     * Expands buffer by 2x when line terminator is not found, but does not exceed max
     * 
     * @param buffer ByteBuffer to store the line
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
        if (max > buffer.getBuffer().length) {
            throw new IllegalArgumentException("Max length cannot exceed buffer length");
        }

        int bytesRead = readLine(buffer.getBuffer(), max);
        if (bytesRead > 0) {
            // Update the ByteBuffer's length to reflect actual bytes read
            buffer.setLength(bytesRead);
        }
        return bytesRead;
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
            ByteBuffer lineBuffer = new ByteBuffer(new byte[maxLineLength]);
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
    private byte[] expandBuffer(byte[] oldBuffer, int max) {
        int newSize = Math.min(oldBuffer.length * 2, max);
        if (newSize <= oldBuffer.length) {
            // Cannot expand further
            return oldBuffer;
        }
        
        byte[] newBuffer = new byte[newSize];
        System.arraycopy(oldBuffer, 0, newBuffer, 0, oldBuffer.length);
        return newBuffer;
    }


}
