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
        assertEquals(HttpStream.NONE, request.getHttpStream());
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
        assertEquals(HttpStream.CONTENT_LENGTH, request.getHttpStream());
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
        assertEquals(HttpStream.CHUNKED, request.getHttpStream());
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
        assertEquals(HttpStream.NONE, request.getHttpStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse WebSocket upgrade request")
    void testParseWebSocketRequest() {
        HeaderLines headers = new HeaderLines(6);
        
        // Add request line
        headers.add(new ByteBuffer("GET /websocket HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Upgrade: websocket".getBytes()));
        headers.add(new ByteBuffer("Connection: Upgrade".getBytes()));
        headers.add(new ByteBuffer("Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==".getBytes()));
        headers.add(new ByteBuffer("Sec-WebSocket-Version: 13".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/websocket", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertEquals("Upgrade", request.getConnection());
        assertNull(request.getContentLength()); // WebSocket doesn't use Content-Length
        assertEquals(HttpStream.WEBSOCKET, request.getHttpStream());
        assertTrue(request.hasBody());
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
        assertEquals(HttpStream.NONE, request.getHttpStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with Connection header")
    void testParseRequestWithConnection() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add request line
        headers.add(new ByteBuffer("GET /test HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Connection: close".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/test", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertEquals("close", request.getConnection());
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(HttpStream.NONE, request.getHttpStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with Content-Length: 0")
    void testParseRequestWithZeroContentLength() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add request line
        headers.add(new ByteBuffer("POST /empty HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 0".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("POST", request.getMethod());
        assertEquals("/empty", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertEquals("example.com", request.getHost());
        assertNull(request.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(0), request.getContentLength());
        assertEquals(HttpStream.CONTENT_LENGTH, request.getHttpStream());
        assertFalse(request.hasBody()); // Content-Length: 0 means no body
    }
    
    @Test
    @DisplayName("Parse invalid request line")
    void testParseInvalidRequestLine() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add invalid request line
        headers.add(new ByteBuffer("INVALIDREQUEST".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Parse request line with missing version")
    void testParseRequestLineMissingVersion() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add request line with missing version
        headers.add(new ByteBuffer("GET /test".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Parse request line with invalid method")
    void testParseRequestLineInvalidMethod() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add request line with invalid method
        headers.add(new ByteBuffer("INVALID /test HTTP/1.1".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpRequest();
        });
    }
    
    @Test
    @DisplayName("Parse request without Host header")
    void testParseRequestWithoutHost() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add request line
        headers.add(new ByteBuffer("GET /test HTTP/1.1".getBytes()));
        
        // Add other header (not Host)
        headers.add(new ByteBuffer("User-Agent: TestAgent".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        assertEquals("GET", request.getMethod());
        assertEquals("/test", request.getPath());
        assertEquals("HTTP/1.1", request.getVersion());
        assertNull(request.getHost()); // No Host header
        assertNull(request.getConnection()); // No Connection header in this test
        assertNull(request.getContentLength()); // No Content-Length header in this test
        assertEquals(HttpStream.NONE, request.getHttpStream());
        assertFalse(request.hasBody());
    }
    
    @Test
    @DisplayName("Parse request with multiple Content-Length headers")
    void testParseRequestWithMultipleContentLength() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add request line
        headers.add(new ByteBuffer("POST /test HTTP/1.1".getBytes()));
        
        // Add headers with multiple Content-Length
        headers.add(new ByteBuffer("Host: example.com".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 100".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 200".getBytes()));
        
        HttpRequest request = headers.parseHttpRequest();
        
        // Should use the first Content-Length header found
        assertEquals(Integer.valueOf(100), request.getContentLength());
    }
}
