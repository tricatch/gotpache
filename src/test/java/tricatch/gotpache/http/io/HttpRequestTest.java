package tricatch.gotpache.http.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpRequest Test")
class HttpRequestTest {

    @Test
    @DisplayName("Parse GET request without body")
    void testParseGetRequest() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add request line
        headers.add(new ByteBuffer("GET /test HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("User-Agent: TestAgent".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/test", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse POST request with Content-Length")
    void testParsePostRequestWithContentLength() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add request line
        headers.add(new ByteBuffer("POST /submit HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 1024".getBytes()));
        headers.add(new ByteBuffer("Content-Type: application/json".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("POST", request.getMethod());
        assertEquals("/submit", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(1024), request.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, request.getBodyStream());
        assertTrue(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse POST request with chunked encoding")
    void testParsePostRequestWithChunked() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add request line
        headers.add(new ByteBuffer("POST /upload HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Transfer-Encoding: chunked".getBytes()));
        headers.add(new ByteBuffer("Content-Type: multipart/form-data".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("POST", request.getMethod());
        assertEquals("/upload", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // Chunked encoding doesn't use Content-Length
        assertEquals(BodyStream.CHUNKED, request.getBodyStream());
        assertTrue(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse HEAD request")
    void testParseHeadRequest() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add request line
        headers.add(new ByteBuffer("HEAD /status HTTP/1.0".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("HEAD", request.getMethod());
        assertEquals("/status", request.getPath());
        assertEquals("HTTP/1.0", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse DELETE request")
    void testParseDeleteRequest() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add request line
        headers.add(new ByteBuffer("DELETE /resource/123 HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("DELETE", request.getMethod());
        assertEquals("/resource/123", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with empty headers")
    void testParseRequestWithEmptyHeaders() {
        HeaderLines headers = new HeaderLines(1);
        
        // Add only request line
        headers.add(new ByteBuffer("GET / HTTP/1.1".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertNull(request.getHost()); // No Host header in this test
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with complex path")
    void testParseRequestWithComplexPath() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add request line with complex path
        headers.add(new ByteBuffer("GET /api/users/123?page=1&size=10 HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/api/users/123?page=1&size=10", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Test invalid request line - no spaces")
    void testInvalidRequestLineNoSpaces() {
        HeaderLines headers = new HeaderLines(1);
        headers.add(new ByteBuffer("INVALIDREQUEST".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Test invalid request line - only one space")
    void testInvalidRequestLineOneSpace() {
        HeaderLines headers = new HeaderLines(1);
        headers.add(new ByteBuffer("GET /path".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Test empty headers")
    void testEmptyHeaders() {
        HeaderLines headers = new HeaderLines(0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Parse request with Connection header")
    void testParseRequestWithConnection() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add request line
        headers.add(new ByteBuffer("GET /test HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/test", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertEquals("keep-alive", request.getConnection());
        assertNull(request.getContentLength());
        assertEquals(BodyStream.NONE, request.getBodyStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with Connection close")
    void testParseRequestWithConnectionClose() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add request line
        headers.add(new ByteBuffer("POST /submit HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Connection: close".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 512".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("POST", request.getMethod());
        assertEquals("/submit", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertEquals("close", request.getConnection());
        assertEquals(Integer.valueOf(512), request.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, request.getBodyStream());
        assertTrue(request.hasBody());
    }
}
