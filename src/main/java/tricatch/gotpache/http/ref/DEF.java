package tricatch.gotpache.http.ref;

public class DEF {

    public static final int MAX_BUFFER = 16; //16K
    public static final int MAX_BUFFER_BYTES = 1024 * MAX_BUFFER;

    public static class HEADER {
        public static final byte[] HOST = "Host".getBytes();
        public static final byte[] CONTENT_LENGTH = "Content-Length".getBytes();
        public static final byte[] TRANSFER_ENCODING = "Transfer-Encoding".getBytes();
        public static final byte[] CONNECTION = "Connection".getBytes();
        public static final byte[] KEEP_ALIVE = "Keep-Alive".getBytes();
    }

    public enum Stream {
        NONE
        , CONTENT_LENGTH
        , CHUNKED
        , WEBSOCKET
        , UNTIL_CLOSE
        , NULL
    }
}
