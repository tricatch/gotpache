package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
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

    public int readHeader() throws IOException {

        int pos = 0;
        headerBufferLen = 0;

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
                logger.trace( "{} H[{}], full\n{}"
                        , this.readerMode
                        , readCount
                        , new String(this.headerBuffer, 0, this.headerBufferLen)
                );
                logger.trace( "{} H[{}], full\n{}"
                        , this.readerMode
                        , readCount
                        , ByteUtils.toHexPretty(this.headerBuffer, 0, this.headerBufferLen)
                );
            }

            int headerStart = 0;

            if( ReaderMode.REQUEST == readerMode){

                headerStart = HeaderParser.parseRequestLine(this.headerBuffer, 0, headerBufferLen, this.requestField );

                if( logger.isDebugEnabled() ) logger.trace( "{} parsedRequestLine {}, {}, {}"
                        , this.readerMode
                        , this.requestField.method(this.headerBuffer)
                        , this.requestField.path(this.headerBuffer)
                        , this.requestField.version(this.headerBuffer)
                );

            } else {

                headerStart = HeaderParser.parseStatusLine(this.headerBuffer, 0, headerBufferLen, this.responseField );

                if( logger.isDebugEnabled() ) logger.trace( "{} parsedResponseLine {}, {}, {}"
                        , this.readerMode
                        , this.responseField.version(this.headerBuffer)
                        , this.responseField.statusCode(this.headerBuffer)
                        , this.responseField.reason(this.headerBuffer)
                );
            }

            int headerSize = HeaderParser.parseHeader(this.headerBuffer, headerStart, headerBufferLen, this.headerFieldList);

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
