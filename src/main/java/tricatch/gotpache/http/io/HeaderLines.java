package tricatch.gotpache.http.io;

import java.util.ArrayList;
import java.io.IOException;
import tricatch.gotpache.http.HTTP;

/**
 * ArrayList extension for storing HTTP header lines
 * Provides convenient methods for header line management
 */
public class HeaderLines extends ArrayList<ByteBuffer> {
    
    /**
     * Constructor with initial capacity
     * @param initialCapacity initial capacity for the list
     */
    public HeaderLines(int initialCapacity) {
        super(initialCapacity);
    }
    
    /**
     * Add header line as ByteBuffer
     * @param headerLine header line ByteBuffer
     * @return true if added successfully
     */
    public boolean addHeaderLine(ByteBuffer headerLine) {
        return add(headerLine);
    }
    

    
    /**
     * Find header line by name using byte array (case-insensitive)
     * @param headerNameBytes header name as byte array
     * @return header line as ByteBuffer, or null if not found
     */
    public ByteBuffer findHeaderLine(byte[] headerNameBytes) {
        // Skip first line (request/response line) and search from second line onwards
        for (int i = 1; i < size(); i++) {
            ByteBuffer headerBuffer = get(i);
            if (startsWithIgnoreCase(headerBuffer.getBuffer(), headerBuffer.getLength(), headerNameBytes) >= 0) {
                return headerBuffer;
            }
        }
        return null;
    }
    

    
    /**
     * Get header value as string by name using byte array (case-insensitive)
     * @param headerNameBytes header name as byte array
     * @return header value as string, or null if not found
     */
    public String getHeaderValueAsString(byte[] headerNameBytes) {
        // Skip first line (request/response line) and search from second line onwards
        for (int i = 1; i < size(); i++) {
            ByteBuffer headerBuffer = get(i);
            int colonIndex = startsWithIgnoreCase(headerBuffer.getBuffer(), headerBuffer.getLength(), headerNameBytes);
            if (colonIndex >= 0 && colonIndex < headerBuffer.getLength() - 1) {
                return trimHeaderValueAsString(headerBuffer.getBuffer(), headerBuffer.getLength(), colonIndex + 1);
            }
        }
        return null;
    }
    
    /**
     * Get header value as integer by name using byte array
     * @param headerNameBytes header name as byte array
     * @return header value as integer, or null if not found or not a number
     */
    public Integer getHeaderValueAsInt(byte[] headerNameBytes) {
        // Skip first line (request/response line) and search from second line onwards
        for (int i = 1; i < size(); i++) {
            ByteBuffer headerBuffer = get(i);
            int colonIndex = startsWithIgnoreCase(headerBuffer.getBuffer(), headerBuffer.getLength(), headerNameBytes);
            if (colonIndex >= 0 && colonIndex < headerBuffer.getLength() - 1) {
                return trimHeaderValueAsInt(headerBuffer.getBuffer(), headerBuffer.getLength(), colonIndex + 1);
            }
        }
        return null;
    }
    
    /**
     * Get header value as long by name using byte array
     * @param headerNameBytes header name as byte array
     * @return header value as long, or null if not found or not a number
     */
    public Long getHeaderValueAsLong(byte[] headerNameBytes) {
        // Skip first line (request/response line) and search from second line onwards
        for (int i = 1; i < size(); i++) {
            ByteBuffer headerBuffer = get(i);
            int colonIndex = startsWithIgnoreCase(headerBuffer.getBuffer(), headerBuffer.getLength(), headerNameBytes);
            if (colonIndex >= 0 && colonIndex < headerBuffer.getLength() - 1) {
                return trimHeaderValueAsLong(headerBuffer.getBuffer(), headerBuffer.getLength(), colonIndex + 1);
            }
        }
        return null;
    }
    
    /**
     * Check if header exists using byte array
     * @param headerNameBytes header name as byte array
     * @return true if header exists
     */
    public boolean hasHeader(byte[] headerNameBytes) {
        return findHeaderLine(headerNameBytes) != null;
    }
    
    /**
     * Check if byte array starts with another byte array (case-insensitive)
     * @param source source byte array
     * @param sourceLength actual length of source data
     * @param prefix prefix byte array to check
     * @return colon index if source starts with prefix, -1 otherwise
     */
    private int startsWithIgnoreCase(byte[] source, int sourceLength, byte[] prefix) {
        if (sourceLength < prefix.length + 1) { // +1 for colon
            return -1;
        }
        
        // Check prefix (case-insensitive)
        for (int i = 0; i < prefix.length; i++) {
            byte sourceByte = source[i];
            byte prefixByte = prefix[i];
            
            // Convert to lowercase for comparison
            if (sourceByte >= 'A' && sourceByte <= 'Z') {
                sourceByte = (byte) (sourceByte + 32);
            }
            if (prefixByte >= 'A' && prefixByte <= 'Z') {
                prefixByte = (byte) (prefixByte + 32);
            }
            
            if (sourceByte != prefixByte) {
                return -1;
            }
        }
        
        // Check for colon after prefix (with optional spaces)
        int colonIndex = prefix.length;
        while (colonIndex < sourceLength && (source[colonIndex] == ' ' || source[colonIndex] == '\t')) {
            colonIndex++;
        }
        return (colonIndex < sourceLength && source[colonIndex] == ':') ? colonIndex : -1;
    }
    

    

    
    /**
     * Trim header value and convert to string (no memory copy)
     * @param headerBytes full header line bytes
     * @param headerLength actual length of header data
     * @param startIndex start index of value (after colon)
     * @return trimmed header value as string, or null if empty
     */
    private String trimHeaderValueAsString(byte[] headerBytes, int headerLength, int startIndex) {
        if (startIndex >= headerLength) {
            return null;
        }
        
        // Find start (skip leading spaces)
        int start = startIndex;
        while (start < headerLength && (headerBytes[start] == ' ' || headerBytes[start] == '\t')) {
            start++;
        }
        
        // Find end (skip trailing spaces)
        int end = headerLength - 1;
        while (end >= start && (headerBytes[end] == ' ' || headerBytes[end] == '\t')) {
            end--;
        }
        
        if (start > end) {
            return null;
        }
        
        // Convert directly to string without copying
        return new String(headerBytes, start, end - start + 1);
    }
    
    /**
     * Trim header value and convert to integer (no memory copy)
     * @param headerBytes full header line bytes
     * @param headerLength actual length of header data
     * @param startIndex start index of value (after colon)
     * @return trimmed header value as integer, or null if not a number
     */
    private Integer trimHeaderValueAsInt(byte[] headerBytes, int headerLength, int startIndex) {
        if (startIndex >= headerLength) {
            return null;
        }
        
        // Find start (skip leading spaces)
        int start = startIndex;
        while (start < headerLength && (headerBytes[start] == ' ' || headerBytes[start] == '\t')) {
            start++;
        }
        
        // Find end (skip trailing spaces)
        int end = headerLength - 1;
        while (end >= start && (headerBytes[end] == ' ' || headerBytes[end] == '\t')) {
            end--;
        }
        
        if (start > end) {
            return null;
        }
        
        // Parse integer directly from byte array
        try {
            return parseIntFromBytes(headerBytes, start, end - start + 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Trim header value and convert to long (no memory copy)
     * @param headerBytes full header line bytes
     * @param headerLength actual length of header data
     * @param startIndex start index of value (after colon)
     * @return trimmed header value as long, or null if not a number
     */
    private Long trimHeaderValueAsLong(byte[] headerBytes, int headerLength, int startIndex) {
        if (startIndex >= headerLength) {
            return null;
        }
        
        // Find start (skip leading spaces)
        int start = startIndex;
        while (start < headerLength && (headerBytes[start] == ' ' || headerBytes[start] == '\t')) {
            start++;
        }
        
        // Find end (skip trailing spaces)
        int end = headerLength - 1;
        while (end >= start && (headerBytes[end] == ' ' || headerBytes[end] == '\t')) {
            end--;
        }
        
        if (start > end) {
            return null;
        }
        
        // Parse long directly from byte array
        try {
            return parseLongFromBytes(headerBytes, start, end - start + 1);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Parse integer from byte array without creating string
     * @param bytes byte array
     * @param offset start offset
     * @param length number of bytes to parse
     * @return parsed integer
     * @throws NumberFormatException if not a valid integer
     */
    private int parseIntFromBytes(byte[] bytes, int offset, int length) {
        if (length == 0) {
            throw new NumberFormatException("Empty string");
        }
        
        int result = 0;
        boolean negative = false;
        int i = offset;
        
        // Check for sign
        if (bytes[i] == '-') {
            negative = true;
            i++;
        } else if (bytes[i] == '+') {
            i++;
        }
        
        if (i >= offset + length) {
            throw new NumberFormatException("No digits");
        }
        
        // Parse digits
        while (i < offset + length) {
            byte b = bytes[i];
            if (b < '0' || b > '9') {
                throw new NumberFormatException("Invalid digit");
            }
            result = result * 10 + (b - '0');
            i++;
        }
        
        return negative ? -result : result;
    }
    
    /**
     * Parse long from byte array without creating string
     * @param bytes byte array
     * @param offset start offset
     * @param length number of bytes to parse
     * @return parsed long
     * @throws NumberFormatException if not a valid long
     */
    private long parseLongFromBytes(byte[] bytes, int offset, int length) {
        if (length == 0) {
            throw new NumberFormatException("Empty string");
        }
        
        long result = 0;
        boolean negative = false;
        int i = offset;
        
        // Check for sign
        if (bytes[i] == '-') {
            negative = true;
            i++;
        } else if (bytes[i] == '+') {
            i++;
        }
        
        if (i >= offset + length) {
            throw new NumberFormatException("No digits");
        }
        
        // Parse digits
        while (i < offset + length) {
            byte b = bytes[i];
            if (b < '0' || b > '9') {
                throw new NumberFormatException("Invalid digit");
            }
            result = result * 10 + (b - '0');
            i++;
        }
        
        return negative ? -result : result;
    }
    
    /**
     * Get total size of all header lines in bytes
     * @return total byte size
     */
    public int getTotalByteSize() {
        int totalSize = 0;
        for (ByteBuffer headerBuffer : this) {
            totalSize += headerBuffer.getLength();
            totalSize += 2; // Add CRLF for each line
        }
        return totalSize;
    }
    
    /**
     * Clear all header lines
     */
    @Override
    public void clear() {
        super.clear();
    }
    
    /**
     * Get header lines as formatted string
     * @return formatted header lines string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (ByteBuffer headerBuffer : this) {
            sb.append(headerBuffer.toString()).append("\r\n");
        }
        return sb.toString();
    }
    
    /**
     * Parse HTTP request from header lines
     * Parses the first line to extract method, path, and version
     * Determines body stream type based on headers
     * @return HttpRequest object with parsed information
     * @throws IllegalArgumentException if request line is invalid
     */
    public HttpRequest parseHttpRequest() {
        if (isEmpty()) {
            throw new IllegalArgumentException("HeaderLines is empty");
        }
        
        // Parse first line (request line)
        ByteBuffer requestLine = get(0);
        byte[] requestBytes = requestLine.getBuffer();
        int requestLength = requestLine.getLength();
        
        // Find first space (separates method and path)
        int firstSpaceIndex = -1;
        for (int i = 0; i < requestLength; i++) {
            if (requestBytes[i] == HTTP.SPACE) {
                firstSpaceIndex = i;
                break;
            }
        }
        
        if (firstSpaceIndex == -1) {
            throw new IllegalArgumentException("Invalid request line: no space found after method");
        }
        
        // Find second space (separates path and version)
        int secondSpaceIndex = -1;
        for (int i = firstSpaceIndex + 1; i < requestLength; i++) {
            if (requestBytes[i] == HTTP.SPACE) {
                secondSpaceIndex = i;
                break;
            }
        }
        
        if (secondSpaceIndex == -1) {
            throw new IllegalArgumentException("Invalid request line: no space found after path");
        }
        
        // Extract method, path, and version
        String method = new String(requestBytes, 0, firstSpaceIndex);
        String path = new String(requestBytes, firstSpaceIndex + 1, secondSpaceIndex - firstSpaceIndex - 1);
        String version = new String(requestBytes, secondSpaceIndex + 1, requestLength - secondSpaceIndex - 1);
        
        // Extract host from headers
        String host = getHeaderValueAsString(HTTP.HEADER.HOST);
        
        // Extract connection from headers
        String connection = getHeaderValueAsString(HTTP.HEADER.CONNECTION);
        
        // Extract content length from headers
        Integer contentLength = getHeaderValueAsInt(HTTP.HEADER.CONTENT_LENGTH);
        
        // Determine body stream type
        BodyStream bodyStream = determineBodyStreamType();
        
        return new HttpRequest(method, path, version, host, connection, contentLength, bodyStream, this);
    }
    
    /**
     * Determine body stream type based on headers
     * @return BodyStream type
     */
    private BodyStream determineBodyStreamType() {
        // Check for Transfer-Encoding: chunked
        if (hasHeader(HTTP.HEADER.TRANSFER_ENCODING)) {
            String transferEncoding = getHeaderValueAsString(HTTP.HEADER.TRANSFER_ENCODING);
            if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                return BodyStream.CHUNKED;
            }
        }
        
        // Check for Content-Length
        if (hasHeader(HTTP.HEADER.CONTENT_LENGTH)) {
            Integer contentLength = getHeaderValueAsInt(HTTP.HEADER.CONTENT_LENGTH);
            if (contentLength != null && contentLength >= 0) {
                return BodyStream.CONTENT_LENGTH;
            }
        }
        
        // Check if method typically has no body
        if (!isEmpty()) {
            ByteBuffer requestLine = get(0);
            byte[] requestBytes = requestLine.getBuffer();
            int requestLength = requestLine.getLength();
            
            // Find method end (first space)
            int firstSpaceIndex = -1;
            for (int i = 0; i < requestLength; i++) {
                if (requestBytes[i] == HTTP.SPACE) {
                    firstSpaceIndex = i;
                    break;
                }
            }
            
            if (firstSpaceIndex > 0) {
                String method = new String(requestBytes, 0, firstSpaceIndex);
                if ("GET".equals(method) || "HEAD".equals(method) || "DELETE".equals(method)) {
                    return BodyStream.NONE;
                }
            }
        }
        
        // Default to no body
        return BodyStream.NONE;
    }
    
    /**
     * Parse HTTP response from header lines
     * Parses the first line to extract version, status code, and status message
     * Determines body stream type based on headers
     * @return HttpResponse object with parsed information
     * @throws IllegalArgumentException if response line is invalid
     */
    public HttpResponse parseHttpResponse() {
        if (isEmpty()) {
            throw new IllegalArgumentException("HeaderLines is empty");
        }
        
        // Parse first line (response line)
        ByteBuffer responseLine = get(0);
        byte[] responseBytes = responseLine.getBuffer();
        int responseLength = responseLine.getLength();
        
        // Find first space (separates version and status code)
        int firstSpaceIndex = -1;
        for (int i = 0; i < responseLength; i++) {
            if (responseBytes[i] == HTTP.SPACE) {
                firstSpaceIndex = i;
                break;
            }
        }
        
        if (firstSpaceIndex == -1) {
            throw new IllegalArgumentException("Invalid response line: no space found after version");
        }
        
        // Find second space (separates status code and status message)
        int secondSpaceIndex = -1;
        for (int i = firstSpaceIndex + 1; i < responseLength; i++) {
            if (responseBytes[i] == HTTP.SPACE) {
                secondSpaceIndex = i;
                break;
            }
        }
        
        if (secondSpaceIndex == -1) {
            throw new IllegalArgumentException("Invalid response line: no space found after status code");
        }
        
        // Extract version, status code, and status message
        String version = new String(responseBytes, 0, firstSpaceIndex);
        String statusCodeStr = new String(responseBytes, firstSpaceIndex + 1, secondSpaceIndex - firstSpaceIndex - 1);
        String statusMessage = new String(responseBytes, secondSpaceIndex + 1, responseLength - secondSpaceIndex - 1);
        
        // Parse status code
        int statusCode;
        try {
            statusCode = Integer.parseInt(statusCodeStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid status code: " + statusCodeStr);
        }
        
        // Extract connection from headers
        String connection = getHeaderValueAsString(HTTP.HEADER.CONNECTION);
        
        // Extract content length from headers
        Integer contentLength = getHeaderValueAsInt(HTTP.HEADER.CONTENT_LENGTH);
        
        // Determine body stream type
        BodyStream bodyStream = determineResponseBodyStreamType(statusCode);
        
        return new HttpResponse(version, statusCode, statusMessage, connection, contentLength, bodyStream, this);
    }
    
    /**
     * Determine response body stream type based on status code and headers
     * @param statusCode HTTP status code
     * @return BodyStream type
     */
    private BodyStream determineResponseBodyStreamType(int statusCode) {
        // Some status codes never have a body
        if (statusCode == 204 || statusCode == 304) {
            return BodyStream.NONE;
        }
        
        // Check for Transfer-Encoding: chunked
        if (hasHeader(HTTP.HEADER.TRANSFER_ENCODING)) {
            String transferEncoding = getHeaderValueAsString(HTTP.HEADER.TRANSFER_ENCODING);
            if (transferEncoding != null && transferEncoding.toLowerCase().contains("chunked")) {
                return BodyStream.CHUNKED;
            }
        }
        
        // Check for Content-Length
        if (hasHeader(HTTP.HEADER.CONTENT_LENGTH)) {
            Integer contentLength = getHeaderValueAsInt(HTTP.HEADER.CONTENT_LENGTH);
            if (contentLength != null && contentLength >= 0) {
                return BodyStream.CONTENT_LENGTH;
            }
        }
        
        // For HEAD requests, responses typically have no body
        // This is handled at the application level, not here
        
        // Default to no body for responses without explicit content length or transfer encoding
        return BodyStream.NONE;
    }
}
