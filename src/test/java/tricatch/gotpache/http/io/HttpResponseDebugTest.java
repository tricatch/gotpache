package tricatch.gotpache.http.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HttpResponse Debug Test")
class HttpResponseDebugTest {

    @Test
    @DisplayName("Debug 304 response")
    void testDebug304Response() {
        HeaderLines headers = new HeaderLines(4);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 304 Not Modified".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("ETag: \"abc123\"".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        System.out.println("Response: " + response);
        System.out.println("Status Code: " + response.getStatusCode());
        System.out.println("Is Successful: " + response.isSuccessful());
        System.out.println("Is Client Error: " + response.isClientError());
        System.out.println("Is Server Error: " + response.isServerError());
        System.out.println("Status Code >= 200: " + (response.getStatusCode() >= 200));
        System.out.println("Status Code < 300: " + (response.getStatusCode() < 300));
        
        // Just check status code for now
        assertEquals(304, response.getStatusCode());
        assertFalse(response.isSuccessful()); // 304 is not successful, it's redirection
        assertTrue(response.isRedirection()); // 304 is redirection
    }
    
    @Test
    @DisplayName("Test 200 response")
    void test200Response() {
        HeaderLines headers = new HeaderLines(3);
        
        // Add response line
        headers.add(new ByteBuffer("HTTP/1.1 200 OK".getBytes()));
        
        // Add headers
        headers.add(new ByteBuffer("Server: nginx/1.18.0".getBytes()));
        headers.add(new ByteBuffer("Connection: keep-alive".getBytes()));
        
        HttpResponse response = headers.parseHttpResponse();
        
        System.out.println("200 Response: " + response);
        System.out.println("200 Status Code: " + response.getStatusCode());
        System.out.println("200 Is Successful: " + response.isSuccessful());
        
        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccessful());
    }
    
    @Test
    @DisplayName("Test direct 304 check")
    void testDirect304Check() {
        // Test the logic directly
        int statusCode = 304;
        boolean isSuccessful = statusCode >= 200 && statusCode < 300;
        boolean isClientError = statusCode >= 400 && statusCode < 500;
        boolean isServerError = statusCode >= 500 && statusCode < 600;
        
        boolean isRedirection = statusCode >= 300 && statusCode < 400;
        
        System.out.println("Direct test - Status Code: " + statusCode);
        System.out.println("Direct test - Is Successful: " + isSuccessful);
        System.out.println("Direct test - Is Redirection: " + isRedirection);
        System.out.println("Direct test - Is Client Error: " + isClientError);
        System.out.println("Direct test - Is Server Error: " + isServerError);
        System.out.println("Direct test - Status Code >= 200: " + (statusCode >= 200));
        System.out.println("Direct test - Status Code < 300: " + (statusCode < 300));
        System.out.println("Direct test - Status Code >= 300: " + (statusCode >= 300));
        System.out.println("Direct test - Status Code < 400: " + (statusCode < 400));
        
        assertFalse(isSuccessful); // 304 is not successful
        assertTrue(isRedirection); // 304 is redirection
        assertFalse(isClientError);
        assertFalse(isServerError);
    }
}
