package tricatch.gotpache.http.io;

/**
 * HTTP message body stream types
 * Defines how the body content should be handled
 */
public enum HttpStream {
    /**
     * No-body stream (null/undefined)
     */
    NULL,
    
    /**
     * No-body content (empty body)
     */
    NONE,
    
    /**
     * Content-Length based body stream
     */
    CONTENT_LENGTH,
    
    /**
     * Chunked transfer encoding body stream
     */
    CHUNKED,
    
    /**
     * WebSocket upgrade body stream
     */
    WEBSOCKET,
    
    /**
     * Body stream until connection close
     */
    UNTIL_CLOSE

    ;

    public static enum Flow {
        REQ,
        RES
    }

    public static enum Connection {
        KEEP_ALIVE,
        CLOSE
    }
}
