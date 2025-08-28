package tricatch.gotpache.http.io;

/**
 * HTTP Response representation
 * Contains parsed response line information and body stream type
 */
public class HttpResponse {
    
    private String version;
    private int statusCode;
    private String statusMessage;
    private String connection;
    private Integer contentLength;
    private BodyStream bodyStream;
    private HeaderLines headers;
    
    /**
     * Constructor with all response information
     * @param version HTTP version
     * @param statusCode response status code
     * @param statusMessage response status message
     * @param connection connection header value
     * @param contentLength content length value
     * @param bodyStream body stream type
     * @param headers response headers
     */
    public HttpResponse(String version, int statusCode, String statusMessage, String connection, Integer contentLength, BodyStream bodyStream, HeaderLines headers) {
        this.version = version;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.connection = connection;
        this.contentLength = contentLength;
        this.bodyStream = bodyStream;
        this.headers = headers;
    }
    
    /**
     * Get HTTP version
     * @return HTTP version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Get response status code
     * @return status code
     */
    public int getStatusCode() {
        return statusCode;
    }
    
    /**
     * Get response status message
     * @return status message
     */
    public String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * Get connection header value
     * @return connection header value
     */
    public String getConnection() {
        return connection;
    }
    
    /**
     * Get content length value
     * @return content length value
     */
    public Integer getContentLength() {
        return contentLength;
    }
    
    /**
     * Get body stream type
     * @return body stream type
     */
    public BodyStream getBodyStream() {
        return bodyStream;
    }
    
    /**
     * Get response headers
     * @return response headers
     */
    public HeaderLines getHeaders() {
        return headers;
    }
    
    /**
     * Check if response has body
     * @return true if response has body
     */
    public boolean hasBody() {
        return bodyStream != BodyStream.NONE && bodyStream != BodyStream.NULL;
    }
    
    /**
     * Check if response is successful (2xx status code)
     * @return true if status code is in 2xx range
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }
    
    /**
     * Check if response is redirection (3xx status code)
     * @return true if status code is in 3xx range
     */
    public boolean isRedirection() {
        return statusCode >= 300 && statusCode < 400;
    }
    
    /**
     * Check if response is client error (4xx status code)
     * @return true if status code is in 4xx range
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }
    
    /**
     * Check if response is server error (5xx status code)
     * @return true if status code is in 5xx range
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }
    
    /**
     * Check if response indicates connection should be closed
     * @return true if connection should be closed
     */
    public boolean shouldCloseConnection() {
        return "close".equalsIgnoreCase(connection) || 
               (version != null && version.equals("HTTP/1.0") && !"keep-alive".equalsIgnoreCase(connection));
    }
    
    /**
     * Get string representation of the response
     * @return formatted response string
     */
    @Override
    public String toString() {
        return String.format("HttpResponse{version='%s', statusCode=%d, statusMessage='%s', connection='%s', contentLength=%s, bodyStream=%s}", 
                           version, statusCode, statusMessage, connection, contentLength, bodyStream);
    }
}
