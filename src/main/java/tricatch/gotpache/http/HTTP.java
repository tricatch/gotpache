package tricatch.gotpache.http;

public class HTTP {

    public static final int INIT_HEADER_LINES = 10;
    public static final int INIT_HEADER_LENGTH = 256;
    public static final int MAX_HEADER_LENGTH = 8 * 1024;
    public static final int BODY_BUFFER_SIZE = 16 * 1024;
    public static final int CHUNK_SIZE_LINE_LENGTH = 128;
    public static final byte[] CRLF = "\r\n".getBytes();
    public static final byte SPACE = (byte)0x20;

    public static class HEADER {
        public static final byte[] HOST = "Host".getBytes();
        public static final byte[] CONTENT_LENGTH = "Content-Length".getBytes();
        public static final byte[] TRANSFER_ENCODING = "Transfer-Encoding".getBytes();
        public static final byte[] CONNECTION = "Connection".getBytes();
    }

}
