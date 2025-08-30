package tricatch.gotpache.http.io;

/**
 * HTTP Request representation
 * Contains parsed request line information and body stream type
 */
public class HttpRequest {
    
    private final String method;
    private final String path;
    private final String version;
    private final String host;
    private final String connection;
    private final Integer contentLength;
    private final HttpStream httpStream;
    private final HeaderLines headers;
    
    /**
     * Constructor with all request information
     * @param method HTTP method (GET, POST, etc.)
     * @param path request path
     * @param version HTTP version
     * @param host request host
     * @param connection connection header value
     * @param contentLength content length value
     * @param httpStream body stream type
     * @param headers request headers
     */
    public HttpRequest(String method, String path, String version, String host, String connection, Integer contentLength, HttpStream httpStream, HeaderLines headers) {
        this.method = method;
        this.path = path;
        this.version = version;
        this.host = host;
        this.connection = connection;
        this.contentLength = contentLength;
        this.httpStream = httpStream;
        this.headers = headers;
    }
    
    /**
     * Get HTTP method
     * @return HTTP method
     */
    public String getMethod() {
        return method;
    }
    
    /**
     * Get request path
     * @return request path
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Get HTTP version
     * @return HTTP version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Get request host
     * @return request host
     */
    public String getHost() {
        return host;
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
    public HttpStream getBodyStream() {
        return httpStream;
    }
    
    /**
     * Get request headers
     * @return request headers
     */
    public HeaderLines getHeaders() {
        return headers;
    }
    
    /**
     * Check if request has body
     * @return true if request has body
     */
    public boolean hasBody() {
        if (httpStream == HttpStream.NONE || httpStream == HttpStream.NULL) {
            return false;
        }
        // Content-Length: 0 means no body even if bodyStream is CONTENT_LENGTH
        if (httpStream == HttpStream.CONTENT_LENGTH && contentLength != null && contentLength == 0) {
            return false;
        }
        return true;
    }
    
    /**
     * Get string representation of the request
     * @return formatted request string
     */
    @Override
    public String toString() {
        return String.format("HttpRequest{method='%s', path='%s', version='%s', host='%s', connection='%s', contentLength=%s, bodyStream=%s}", 
                           method, path, version, host, connection, contentLength, httpStream);
    }
}
