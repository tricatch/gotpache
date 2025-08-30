package tricatch.gotpache.http;

public class HTTP {

    public static final int INIT_HEADER_LINES = 10;
    public static final int INIT_HEADER_LENGTH = 256;
    public static final int MAX_HEADER_LENGTH = 8 * 1024;
    public static final int BODY_BUFFER_SIZE = 16 * 1024;

    public static final int CHUNK_SIZE_LINE_LENGTH = 128;


    public static class HEADER {
        public static final byte[] HOST = "Host".getBytes();
        public static final byte[] CONTENT_LENGTH = "Content-Length".getBytes();
        public static final byte[] TRANSFER_ENCODING = "Transfer-Encoding".getBytes();
        public static final byte[] CONNECTION = "Connection".getBytes();
        public static final byte[] KEEP_ALIVE = "Keep-Alive".getBytes();
    }

    public enum STREAM {
        NONE
        , CONTENT_LENGTH
        , CHUNKED
        , WEBSOCKET
        , UNTIL_CLOSE
        , NULL
    }


    public static final int PIPE_REQ_WAIT_SLEEP = 50;
    public static final int PIPE_REQ_WAIT_MAX = 10;

    public static final int PIPE_RES_WAIT_SLEEP = 50;
    public static final int PIPE_RES_WAIT_MAX = 10;

    public static final String HEADER_CONTENT_LENGTH = "Content-Length:".toLowerCase();
    public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding:".toLowerCase();
    public static final String HEADER_PROXY_CONNECTION = "Proxy-Connection:".toLowerCase();
    public static final String HEADER_UPGRADE_INSECURE_REQUEST = "Upgrade-Insecure-Requests:".toLowerCase();
    public static final String HEADER_CONNECTION = "Connection:".toLowerCase();
    public static final String HEADER_CONNECTION_UPGRADE = "Connection: Upgrade".toLowerCase();
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding:".toLowerCase();
    public static final String HEADER_HOST = "Host:".toLowerCase();

    public static final String TRENC_CHUNKED = "chunked".toLowerCase();

    public static final byte[] CRLF = "\r\n".getBytes();

    public static final byte SPACE = (byte)0x20;

    public static final byte[] BUF_HEADER_CONNECTION_CLOSE = "Connection: close\r\n".getBytes();
    public static final byte[] BUF_RESPONSE_CONNECTION_ESTABLISHED = "HTTP/1.0 200 Connection Established\r\nConnection: close\r\n\r\n".getBytes();
}
