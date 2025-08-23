package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.HttpHeader;
import tricatch.gotpache.http.field.HeaderField;
import tricatch.gotpache.http.field.RequestField;
import tricatch.gotpache.http.field.ResponseField;
import tricatch.gotpache.http.parser.HeaderParser;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HttpBufferedReader {

    private static Logger logger = LoggerFactory.getLogger( HttpBufferedReader.class );

    private ReaderMode readerMode = null;
    private InputStream in = null;
    private boolean eof = false;
    private byte[] headerBuffer = new byte[HTTP.HEADER_BUFFER_SIZE];
    private byte[] bodyBuffer = new byte[HTTP.BODY_BUFFER_SIZE];

    private RequestField requestField = new RequestField();
    private ResponseField responseField = new ResponseField();
    private List<HeaderField> headerFieldList = new ArrayList<>();
    private int bodyBufferLen = 0;
    private int headerBufferLen = 0;

    private String method = null;
    private String path = null;
    private String version = null;
    private int status = 0;
    private String reason = null;

    private String host = null;

    private BodyMode bodyMode = BodyMode.NULL;
    private int contentLength = 0;


    public HttpBufferedReader(ReaderMode readerMode, InputStream in){
        this.readerMode = readerMode;
        this.in = in;
    }

    public byte[] getHeaderBuffer(){
        return this.headerBuffer;
    }

    public int getHeaderBufferLen(){
        return this.headerBufferLen;
    }

    public boolean isEof() {
        return eof;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public String getVersion() {
        return version;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getHost(){
        return host;
    }

    public int readHeader() throws IOException {

        int pos = 0;

        this.headerBufferLen = 0;
        this.bodyMode = BodyMode.NULL;
        this.method = null;
        this.path = null;
        this.version = null;
        this.status = 0;
        this.reason = null;
        this.contentLength = 0;

        for (int readCount=1;;readCount++) {

            int remaining = this.headerBuffer.length - pos;

            if (remaining <= 0) {
                throw new IOException("Header too large (exceeds 16KB)");
            }

            //int read = in.read(this.headerBuffer, pos, remaining);

            byte[] temp = new byte[10];
            int read = in.read(temp);

            if( logger.isTraceEnabled() ) logger.trace( "{} H[{}], read {}", this.readerMode, readCount, read );

            if (read == -1){
                this.eof = true;
                return -1;
            }

            System.arraycopy(temp, 0, this.headerBuffer, pos, read);

            int idx = ByteUtils.indexOfCRLFCRLF(this.headerBuffer, pos, pos+read);

            pos += read;

            if( idx < 0 ) continue;

            pos += read;
            idx = idx + 4; //CRLFCRLF

            headerBufferLen = idx;

            //remain data - copy to bodyBuffer
            int extraLen = pos - idx;

            if (extraLen > 0) {
                System.arraycopy(this.headerBuffer, idx, this.bodyBuffer, 0, extraLen);
                this.bodyBufferLen = extraLen;

                if (logger.isTraceEnabled()) {
                    logger.trace("HTTP header end found at {}, extra body {} bytes copied to bodyBuffer",
                            idx, extraLen);
                }

            } else {
                this.bodyBufferLen = 0;
            }

            if( logger.isTraceEnabled() ){
                logger.trace( "{} HEADERS\n{}"
                        , this.readerMode
                        , new String(this.headerBuffer, 0, this.headerBufferLen)
                );
                logger.trace( "{} HEADERS - RAW\n{}"
                        , this.readerMode
                        , ByteUtils.toHexPretty(this.headerBuffer, 0, this.headerBufferLen)
                );
            }

            int headerStart = 0;

            if( ReaderMode.REQUEST == readerMode){

                headerStart = HeaderParser.parseRequestLine(this.headerBuffer, 0, headerBufferLen, this.requestField );

                this.method = this.requestField.method(this.headerBuffer);
                this.path = this.requestField.path(this.headerBuffer);
                this.version = this.requestField.version(this.headerBuffer);

                if( logger.isDebugEnabled() ) logger.trace( "{} parsedRequestLine ( {}, {}, {} )"
                        , this.readerMode
                        , this.method
                        , this.path
                        , this.version
                );

            } else {

                headerStart = HeaderParser.parseStatusLine(this.headerBuffer, 0, headerBufferLen, this.responseField );

                this.version = this.responseField.version(this.headerBuffer);
                this.status = Integer.parseInt(this.responseField.statusCode(this.headerBuffer));
                this.reason = this.responseField.reason(this.headerBuffer);

                if( logger.isDebugEnabled() ) logger.trace( "{} parsedResponseLine ( {}, {}, {} )"
                        , this.readerMode
                        , this.version
                        , this.status
                        , this.reason
                );
            }

            int headerSize = HeaderParser.parseHeader(this.headerBuffer, headerStart, headerBufferLen, this.headerFieldList);

            if( logger.isDebugEnabled() ) logger.trace("{} parsedHeaderSize {}"
                    , this.readerMode
                    , headerSize
            );

            if( ReaderMode.REQUEST == this.readerMode ){

                HeaderField hostField = HeaderParser.findHeader(this.headerFieldList
                        , this.headerBuffer
                        , HttpHeader.HOST
                );

                this.host = hostField.value(this.headerBuffer);
                if( logger.isTraceEnabled() ) logger.trace("{} find-header, Host : {}", this.readerMode, this.host);

                switch (this.method ){
                    case "GET" :
                    case "HEAD" :
                    case "OPTIONS" :
                    case "DELETE" :
                    case "TRACE" :
                        this.bodyMode = BodyMode.NONE;
                        break;
                }

            } else {

                if ((this.status >= 100 && this.status < 200)
                        || this.status == 204
                        || this.status == 304) {

                    this.bodyMode = BodyMode.NONE;
                }

            }

            //chunked
            if( BodyMode.NULL == this.bodyMode ){
                HeaderField transferEncodingField = HeaderParser.findHeader(this.headerFieldList
                        , this.headerBuffer
                        , HttpHeader.TRANSFER_ENCODING
                );
                if( transferEncodingField!=null ){
                    String transferEncodingVal = transferEncodingField.value(this.headerBuffer);
                    if( "chunked".equals(transferEncodingVal) ) this.bodyMode = BodyMode.CHUNKED;
                    if( logger.isTraceEnabled() ) logger.trace("{}, Transfer-Encoding : {}", this.readerMode, transferEncodingVal);
                } else {
                    if( logger.isTraceEnabled() ) logger.trace("{}, Transfer-Encoding : NULL", this.readerMode);
                }
            }

            //content-length
            if( BodyMode.NULL == this.bodyMode ) {
                HeaderField contentLengthField = HeaderParser.findHeader(this.headerFieldList
                        , this.headerBuffer
                        , HttpHeader.CONTENT_LENGTH
                );
                if( contentLengthField!=null ){
                    this.contentLength = Integer.parseInt(contentLengthField.value(this.headerBuffer));
                    this.bodyMode = BodyMode.CONTENT_LENGTH;
                    if( logger.isTraceEnabled() ) logger.trace("{}, Content-Length : {}", this.readerMode, this.contentLength);
                } else {
                    if( logger.isTraceEnabled() ) logger.trace("{}, Content-Length : NULL", this.readerMode);
                }
            }

            if( logger.isTraceEnabled() ) logger.trace( "{} bodyMode : {}", this.readerMode, this.bodyMode);

            return idx;
        }
    }

//    private void parseHeaders() {
//        int pos = 0;
//
//        // ---------------- 첫 번째 라인 (RequestLine) ----------------
//        int lineEnd = indexOfCRLF(pos, headerEnd);
//        if (lineEnd > 0) {
//            // 공백 기준으로 method, path, version 나누기
//            int firstSpace = -1, secondSpace = -1;
//            for (int i = pos; i < lineEnd; i++) {
//                if (buffer[i] == ' ') {
//                    if (firstSpace < 0) firstSpace = i;
//                    else { secondSpace = i; break; }
//                }
//            }
//            if (firstSpace > 0 && secondSpace > firstSpace) {
//                requestLine = new RequestField(
//                        pos, firstSpace,                // method
//                        firstSpace + 1, secondSpace,   // path
//                        secondSpace + 1, lineEnd        // version
//                );
//            }
//            pos = lineEnd + 2;
//        }
//
//        // ---------------- 일반 헤더 라인 ----------------
//        while (pos < headerEnd) {
//            if (buffer[pos] == '\r' && buffer[pos + 1] == '\n') break;
//
//            int colon = -1;
//            for (int i = pos; i < headerEnd; i++) {
//                if (buffer[i] == ':') { colon = i; break; }
//            }
//            if (colon < 0) break;
//
//            lineEnd = indexOfCRLF(colon + 1, headerEnd);
//            if (lineEnd < 0) break;
//
//            int keyStart = pos;
//            int keyEnd = colon;
//            int valueStart = colon + 1;
//            int valueEnd = lineEnd;
//
//            while (valueStart < valueEnd && (buffer[valueStart] == ' ' || buffer[valueStart] == '\t')) valueStart++;
//            while (valueEnd > valueStart && (buffer[valueEnd - 1] == ' ' || buffer[valueEnd - 1] == '\t')) valueEnd--;
//
//            headers.add(new HeaderField(keyStart, keyEnd, valueStart, valueEnd));
//
//            pos = lineEnd + 2;
//        }
//    }

}
