package tricatch.gotpache.http.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpResponse Test")
class HttpResponseTest {

    @Test
    @DisplayName("Parse 200 OK response")
    void testParseOkResponse() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 1024".getBytes()));
        headers.add(new ByteBuffer("Content-Type: text/html".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(1024), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection()); // HTTP/1.1 without Connection header
    }
    
    @Test
    @DisplayName("Parse 404 Not Found response")
    void testParseNotFoundResponse() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 404 Not Found".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 0".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(404, response.getStatusCode());
        assertEquals("Not Found", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(0), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertFalse(response.hasBody()); // Content-Length: 0 means no body
        assertFalse(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertTrue(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection()); // HTTP/1.1 without Connection header
    }
    
    @Test
    @DisplayName("Parse 500 Internal Server Error response")
    void testParseServerErrorResponse() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.0 500 Internal Server Error".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 0".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.0", response.getVersion());
        assertEquals(500, response.getStatusCode());
        assertEquals("Internal Server Error", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(0), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertFalse(response.hasBody()); // Content-Length: 0 means no body
        assertFalse(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertFalse(response.isClientError());
        assertTrue(response.isServerError());
        assertTrue(response.shouldCloseConnection()); // HTTP/1.0 without keep-alive
    }
    
    @Test
    @DisplayName("Parse 204 No Content response")
    void testParseNoContentResponse() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 204 No Content".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(204, response.getStatusCode());
        assertEquals("No Content", response.getStatusMessage());
        assertEquals("keep-alive", response.getConnection());
        assertNull(response.getContentLength());
        assertEquals(BodyStream.NONE, response.getBodyStream());
        assertFalse(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.shouldCloseConnection());
    }
    
    @Test
    @DisplayName("Parse 304 Not Modified response")
    void testParseNotModifiedResponse() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 304 Not Modified".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("ETag: \"abc123\"".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(304, response.getStatusCode());
        assertEquals("Not Modified", response.getStatusMessage());
        assertEquals("keep-alive", response.getConnection());
        assertNull(response.getContentLength());
        assertEquals(BodyStream.NONE, response.getBodyStream());
        assertFalse(response.hasBody());
        assertFalse(response.isSuccessful()); // 304 is not successful, it's redirection
        assertTrue(response.isRedirection()); // 304 is redirection
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection());
    }
    
    @Test
    @DisplayName("Parse response with connection close")
    void testParseResponseWithConnectionClose() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 1024".getBytes()));
        headers.add(new ByteBuffer("Connection: close".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("close", response.getConnection());
        assertEquals(Integer.valueOf(1024), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody());
        assertTrue(response.isSuccessful());
        assertTrue(response.shouldCloseConnection());
    }
    
    @Test
    @DisplayName("Parse response with complex status message")
    void testParseResponseWithComplexStatusMessage() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line with complex status message
        headers.add(new ByteBuffer("HTTP/1.1 418 I'm a teapot".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 0".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(418, response.getStatusCode());
        assertEquals("I'm a teapot", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertEquals(Integer.valueOf(0), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertFalse(response.hasBody()); // Content-Length: 0 means no body
        assertFalse(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertTrue(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection()); // HTTP/1.1 without Connection header
    }
    
    @Test
    @DisplayName("Parse invalid response line")
    void testParseInvalidResponseLine() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add invalid response line
        headers.add(new ByteBuffer("INVALIDRESPONSE".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Parse response line with missing status code")
    void testParseResponseLineMissingStatusCode() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add response line with missing status code
        headers.add(new ByteBuffer("HTTP/1.1 200".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Parse response line with invalid status code")
    void testParseResponseLineInvalidStatusCode() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add response line with invalid status code
        headers.add(new ByteBuffer("HTTP/1.1 ABC OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Parse response without Content-Length header")
    void testParseResponseWithoutContentLength() {
        HeaderLines headers = new HeaderLines(2);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers (no Content-Length)
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertNull(response.getContentLength()); // No Content-Length header in this test
        assertEquals(BodyStream.NONE, response.getBodyStream());
        assertFalse(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection()); // HTTP/1.1 without Connection header
    }
    
    @Test
    @DisplayName("Parse response with Transfer-Encoding: chunked")
    void testParseResponseWithChunkedEncoding() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Transfer-Encoding: chunked".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertNull(response.getConnection()); // No Connection header in this test
        assertNull(response.getContentLength()); // Chunked encoding doesn't use Content-Length
        assertEquals(BodyStream.CHUNKED, response.getBodyStream());
        assertTrue(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.isRedirection());
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
        assertFalse(response.shouldCloseConnection()); // HTTP/1.1 without Connection header
    }
    
    @Test
    @DisplayName("Parse response with multiple Content-Length headers")
    void testParseResponseWithMultipleContentLength() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers with multiple Content-Length
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 100".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 200".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        // Should use the first Content-Length header found
        assertEquals(Integer.valueOf(100), response.getContentLength());
    }
}
