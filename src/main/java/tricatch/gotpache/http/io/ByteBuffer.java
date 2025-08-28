package tricatch.gotpache.http.io;

/**
 * Wrapper class for byte array with length
 * Ensures byte array and its length always travel together
 */
public class ByteBuffer {
    
    private final byte[] buffer;
    private int length;
    
    /**
     * Constructor with buffer and length
     * @param buffer byte array
     * @param length actual length of data in buffer
     */
    public ByteBuffer(byte[] buffer, int length) {
        this.buffer = buffer;
        this.length = length;
    }
    
    /**
     * Constructor with buffer (assumes full buffer is used)
     * @param buffer byte array
     */
    public ByteBuffer(byte[] buffer) {
        this.buffer = buffer;
        this.length = buffer.length;
    }
    
    /**
     * Get the byte array
     * @return byte array
     */
    public byte[] getBuffer() {
        return buffer;
    }
    
    /**
     * Get the actual length of data
     * @return length of data
     */
    public int getLength() {
        return length;
    }
    
    /**
     * Set the actual length of data
     * @param length new length
     */
    public void setLength(int length) {
        if (length < 0 || length > buffer.length) {
            throw new IllegalArgumentException("Length must be between 0 and " + buffer.length);
        }
        this.length = length;
    }
    
    /**
     * Get byte at specified index
     * @param index index to get byte from
     * @return byte at index
     * @throws IndexOutOfBoundsException if index is out of bounds
     */
    public byte get(int index) {
        if (index < 0 || index >= length) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Length: " + length);
        }
        return buffer[index];
    }
    
    /**
     * Check if buffer is empty
     * @return true if length is 0
     */
    public boolean isEmpty() {
        return length == 0;
    }
    
    /**
     * Create a new ByteBuffer with a subset of this buffer
     * @param offset start offset
     * @param len length of subset
     * @return new ByteBuffer with subset
     * @throws IndexOutOfBoundsException if offset or length is invalid
     */
    public ByteBuffer subBuffer(int offset, int len) {
        if (offset < 0 || len < 0 || offset + len > length) {
            throw new IndexOutOfBoundsException("Offset: " + offset + ", Length: " + len + ", Buffer length: " + length);
        }
        return new ByteBuffer(buffer, len);
    }
    
    /**
     * Create a new ByteBuffer with data from offset to end
     * @param offset start offset
     * @return new ByteBuffer from offset to end
     * @throws IndexOutOfBoundsException if offset is invalid
     */
    public ByteBuffer subBuffer(int offset) {
        return subBuffer(offset, length - offset);
    }
    
    /**
     * Convert to string using the actual length
     * @return string representation of buffer data
     */
    @Override
    public String toString() {
        return new String(buffer, 0, length);
    }
    
    /**
     * Convert to string with specified charset
     * @param charsetName charset name
     * @return string representation of buffer data
     * @throws java.io.UnsupportedEncodingException if charset is not supported
     */
    public String toString(String charsetName) throws java.io.UnsupportedEncodingException {
        return new String(buffer, 0, length, charsetName);
    }
    
    /**
     * Copy buffer data to another byte array
     * @param dest destination array
     * @param destOffset destination offset
     * @throws IndexOutOfBoundsException if dest array is too small
     */
    public void copyTo(byte[] dest, int destOffset) {
        if (destOffset < 0 || destOffset + length > dest.length) {
            throw new IndexOutOfBoundsException("Destination offset: " + destOffset + ", Destination length: " + dest.length + ", Source length: " + length);
        }
        System.arraycopy(buffer, 0, dest, destOffset, length);
    }
    
    /**
     * Create a copy of this ByteBuffer
     * @return new ByteBuffer with copied data
     */
    public ByteBuffer copy() {
        byte[] newBuffer = new byte[length];
        System.arraycopy(buffer, 0, newBuffer, 0, length);
        return new ByteBuffer(newBuffer, length);
    }
    
    /**
     * Check if this buffer equals another ByteBuffer
     * @param obj object to compare
     * @return true if buffers are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ByteBuffer that = (ByteBuffer) obj;
        
        if (length != that.length) return false;
        
        for (int i = 0; i < length; i++) {
            if (buffer[i] != that.buffer[i]) return false;
        }
        
        return true;
    }
    
    /**
     * Generate hash code
     * @return hash code
     */
    @Override
    public int hashCode() {
        int result = 1;
        for (int i = 0; i < length; i++) {
            result = 31 * result + buffer[i];
        }
        return result;
    }
}
