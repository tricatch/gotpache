package tricatch.gotpache.event;

import tricatch.gotpache.http.io.HeaderLines;
import tricatch.gotpache.http.io.HttpStream;

/**
 * HTTP event for queue processing
 * Created from HTTP request/response, flows through EventQueue → WorkerPool → IP Subscriber / DropConsumer
 */
public class HttpEvent {

    private String clientId;               // Client IP identifier
    private String rid;                    // Request ID
    private HttpEventType type;            // Event type
    private HeaderLines headers;           // Headers (for REQ_HEADER, RES_HEADER)
    private byte[] body;                   // Body data (for REQ_BODY, RES_BODY)
    private long timestamp;                // Event occurrence time
    private HttpStream httpStream;         // Body stream type (CHUNKED, CONTENT_LENGTH, etc.)
    private Integer opcode;                 // WebSocket opcode (for WS_FRAME)
    private String wsDirection;            // WebSocket direction: "REQ" or "RES" (for WS_FRAME)

    public HttpEvent(String clientId, String rid, HttpEventType type) {
        this.clientId = clientId;
        this.rid = rid;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getRid() {
        return rid;
    }

    public void setRid(String rid) {
        this.rid = rid;
    }

    public HttpEventType getType() {
        return type;
    }

    public void setType(HttpEventType type) {
        this.type = type;
    }

    public HeaderLines getHeaders() {
        return headers;
    }

    public void setHeaders(HeaderLines headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public HttpStream getHttpStream() {
        return httpStream;
    }

    public void setHttpStream(HttpStream httpStream) {
        this.httpStream = httpStream;
    }

    public Integer getOpcode() {
        return opcode;
    }

    public void setOpcode(Integer opcode) {
        this.opcode = opcode;
    }

    public String getWsDirection() {
        return wsDirection;
    }

    public void setWsDirection(String wsDirection) {
        this.wsDirection = wsDirection;
    }
}
