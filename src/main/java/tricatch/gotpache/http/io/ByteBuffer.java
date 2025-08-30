package tricatch.gotpache.http.io;

/**
 * Wrapper class for byte array with length
 * Ensures byte array and its length always travel together
 */
public class ByteBuffer {
    
    private byte[] buffer;
    private int length;
    
    /**
     * Constructor with buffer and length
     * @param length actual length of data in buffer
     */
    public ByteBuffer(int length) {
        this.buffer = new byte[length];
    }
    
    /**
     * Constructor with byte array
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

    public void setBuffer(byte[] buffer){
        this.buffer = buffer;
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
            throw new IllegalArgumentException("Length must be between 0 and " + buffer.length + ", length=" + length);
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
     * Convert to string using the actual length
     * @return string representation of buffer data
     */
    @Override
    public String toString() {
        return new String(buffer, 0, length);
    }

}
