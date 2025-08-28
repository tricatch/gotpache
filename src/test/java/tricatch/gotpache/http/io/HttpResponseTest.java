package tricatch.gotpache.http.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpResponse Test")
class HttpResponseTest {

    @Test
    @DisplayName("Parse successful response without body")
    void testParseSuccessfulResponse() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Date: Mon, 01 Jan 2024 12:00:00 GMT".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertNull(response.getConnection());
        assertNull(response.getContentLength());
        assertEquals(BodyStream.NONE, response.getBodyStream());
        assertFalse(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.isClientError());
        assertFalse(response.isServerError());
    }
    
    @Test
    @DisplayName("Parse response with Content-Length")
    void testParseResponseWithContentLength() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Type: text/html".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 1024".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("keep-alive", response.getConnection());
        assertEquals(Integer.valueOf(1024), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.shouldCloseConnection());
    }
    
    @Test
    @DisplayName("Parse response with chunked encoding")
    void testParseResponseWithChunked() {
        HeaderLines headers = new HeaderLines(5);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Transfer-Encoding: chunked".getBytes()));
        headers.add(new ByteBuffer("Content-Type: application/json".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(200, response.getStatusCode());
        assertEquals("OK", response.getStatusMessage());
        assertEquals("keep-alive", response.getConnection());
        assertNull(response.getContentLength()); // Chunked encoding doesn't use Content-Length
        assertEquals(BodyStream.CHUNKED, response.getBodyStream());
        assertTrue(response.hasBody());
        assertTrue(response.isSuccessful());
        assertFalse(response.shouldCloseConnection());
    }
    
    @Test
    @DisplayName("Parse 404 Not Found response")
    void testParseNotFoundResponse() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 404 Not Found".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Type: text/html".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 162".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.1", response.getVersion());
        assertEquals(404, response.getStatusCode());
        assertEquals("Not Found", response.getStatusMessage());
        assertNull(response.getConnection());
        assertEquals(Integer.valueOf(162), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody());
        assertFalse(response.isSuccessful());
        assertTrue(response.isClientError());
        assertFalse(response.isServerError());
    }
    
    @Test
    @DisplayName("Parse 500 Internal Server Error response")
    void testParseServerErrorResponse() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.0 500 Internal Server Error".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Content-Type: text/html".getBytes()));
        headers.add(new ByteBuffer("Content-Length: 200".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        assertEquals("HTTP/1.0", response.getVersion());
        assertEquals(500, response.getStatusCode());
        assertEquals("Internal Server Error", response.getStatusMessage());
        assertNull(response.getConnection());
        assertEquals(Integer.valueOf(200), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody());
        assertFalse(response.isSuccessful());
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
        assertNull(response.getConnection());
        assertEquals(Integer.valueOf(0), response.getContentLength());
        assertEquals(BodyStream.CONTENT_LENGTH, response.getBodyStream());
        assertTrue(response.hasBody()); // Content-Length: 0 is still considered having a body
        assertFalse(response.isSuccessful());
        assertTrue(response.isClientError());
    }
    
    @Test
    @DisplayName("Test invalid response line - no spaces")
    void testInvalidResponseLineNoSpaces() {
        HeaderLines headers = new HeaderLines(1);
        headers.add(new ByteBuffer("INVALIDRESPONSE".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Test invalid response line - only one space")
    void testInvalidResponseLineOneSpace() {
        HeaderLines headers = new HeaderLines(1);
        headers.add(new ByteBuffer("HTTP/1.1 200".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Test invalid status code")
    void testInvalidStatusCode() {
        HeaderLines headers = new HeaderLines(1);
        headers.add(new ByteBuffer("HTTP/1.1 ABC OK".getBytes()));
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
    
    @Test
    @DisplayName("Test empty headers")
    void testEmptyHeaders() {
        HeaderLines headers = new HeaderLines(0);
        
        assertThrows(IllegalArgumentException.class, () -> {
            headers.parseHttpResponse();
        });
    }
}
