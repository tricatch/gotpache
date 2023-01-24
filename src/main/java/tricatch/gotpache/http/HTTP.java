package tricatch.gotpache.http;

public class HTTP {

    public static final int HEADER_FULL_SIZE = 1024 * 16;
    public static final int BODY_BUFFER_SIZE = 1024 * 8;

    public static final int PIPE_REQ_WAIT_SLEEP = 50;
    public static final int PIPE_REQ_WAIT_MAX = 10;

    public static final int PIPE_RES_WAIT_SLEEP = 50;
    public static final int PIPE_RES_WAIT_MAX = 10;

    public static final String HEADER_CONTENT_LENGTH = "Content-Length:".toLowerCase();
    public static final String HEADER_TRANSFER_ENCODING = "Transfer-Encoding:".toLowerCase();
    public static final String HEADER_PROXY_CONNECTION = "Proxy-Connection:".toLowerCase();
    public static final String HEADER_UPGRADE_INSECURE_REQUEST = "Upgrade-Insecure-Requests:".toLowerCase();
    public static final String HEADER_CONNECTION = "Connection:".toLowerCase();
    public static final String HEADER_ACCEPT_ENCODING = "Accept-Encoding:".toLowerCase();
    public static final String HEADER_HOST = "Host:".toLowerCase();

    public static final String TRENC_CHUNKED = "chunked".toLowerCase();

    public static final byte[] CRLF = "\r\n".getBytes();

    public static final byte SPACE = (byte)0x20;

    public static final byte[] BUF_HEADER_CONNECTION_CLOSE = "Connection: close\r\n".getBytes();
    public static final byte[] BUF_RESPONSE_CONNECTION_ESTABLISHED = "HTTP/1.0 200 Connection Established\r\nConnection: close\r\n\r\n".getBytes();
}
