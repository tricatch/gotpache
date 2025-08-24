package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.HttpHeader;
import tricatch.gotpache.http.field.Chunk;
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

    private StreamMode streamMode = null;
    private InputStream in = null;
    private boolean eof = false;
    private byte[] headerBuffer = new byte[HTTP.HEADER_BUFFER_SIZE];
    private byte[] bodyBuffer = new byte[HTTP.BODY_BUFFER_SIZE];

    private RequestField requestField = new RequestField();
    private ResponseField responseField = new ResponseField();
    private List<HeaderField> headerFieldList = new ArrayList<>();

    private int headerBufferLen = 0;

    private int bodyBufferPos = 0;
    private int bodyBufferLen = 0;


    private String method = null;
    private String path = null;
    private String version = null;
    private int status = 0;
    private String reason = null;

    private String host = null;

    private BodyMode bodyMode = BodyMode.NULL;
    private int contentLength = 0;


    public HttpBufferedReader(StreamMode streamMode, InputStream in){
        this.streamMode = streamMode;
        this.in = in;
    }

    public void close() throws IOException {
        if( this.in !=null ) this.in.close();
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

    public BodyMode getBodyMode(){
        return bodyMode;
    }

    public byte[] getBodyBuffer(){
        return bodyBuffer;
    }
    public int getBodyBufferPos() {
        return bodyBufferPos;
    }

    public void moveBodyBufferPosBy(int len){
        this.bodyBufferPos += len;
    }

    public int getBodyBufferLen() {
        return bodyBufferLen;
    }

    public int readHeader() throws IOException {

        int lastPos = 0;

        this.headerBufferLen = 0;
        this.bodyMode = BodyMode.NULL;
        this.method = null;
        this.path = null;
        this.version = null;
        this.status = 0;
        this.reason = null;
        this.contentLength = 0;

        //read-header
        for (int readCount=1;;readCount++) {

            int remaining = this.headerBuffer.length - lastPos;
            if (remaining <= 0) {
                throw new IOException("Header too large (exceeds 16KB)");
            }

            //int read = in.read(this.headerBuffer, pos, remaining);
            //임시테스트용
            byte[] temp = new byte[10];
            int read = in.read(temp);
            if (logger.isTraceEnabled()) logger.trace("{} H[{}], read {}", this.streamMode, readCount, read);
            if (read == -1) {
                this.eof = true;
                return -1;
            }

            //임시테스트용
            System.arraycopy(temp, 0, this.headerBuffer, headerBufferLen, read);
            lastPos = headerBufferLen;
            headerBufferLen += read;

            int endOfHeader = ByteUtils.indexOfCRLFCRLF(this.headerBuffer, lastPos, headerBufferLen);

            if ( endOfHeader > 0 ) {

                endOfHeader += 4; //CRLFCRLF

                bodyBufferLen = headerBufferLen - endOfHeader;
                headerBufferLen = endOfHeader;

                if( bodyBufferLen>0 ) {
                    System.arraycopy(this.headerBuffer, headerBufferLen, this.bodyBuffer, 0, bodyBufferLen);

                    if (logger.isTraceEnabled()) {
                        logger.trace("{} header-length : {}, copy-body : {}"
                                , streamMode
                                , headerBufferLen
                                , bodyBufferLen
                        );
                    }

                } else {

                    bodyBufferLen = 0;
                    if( logger.isTraceEnabled() ) {
                        logger.trace("{} header-length : {}", streamMode, headerBufferLen);
                    }
                }

                break;
            }

        } // read-header

        if( logger.isTraceEnabled() ){
            logger.trace( "{} HEADERS - RAW\n{}"
                    , this.streamMode
                    , ByteUtils.toHexPretty(this.headerBuffer, 0, this.headerBufferLen)
            );
        }


        //parse-header
        int headerStart = 0;

        if( StreamMode.REQUEST == streamMode){

            headerStart = HeaderParser.parseRequestLine(this.headerBuffer, 0, headerBufferLen, this.requestField );

            this.method = this.requestField.method(this.headerBuffer);
            this.path = this.requestField.path(this.headerBuffer);
            this.version = this.requestField.version(this.headerBuffer);

            if( logger.isDebugEnabled() ) logger.trace( "{} parsedRequestLine ( {}, {}, {} )"
                    , this.streamMode
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
                    , this.streamMode
                    , this.version
                    , this.status
                    , this.reason
            );
        }

        int headerSize = HeaderParser.parseHeader(this.headerBuffer, headerStart, headerBufferLen, this.headerFieldList);

        if( logger.isDebugEnabled() ) logger.trace("{} parsedHeaderSize {}"
                , this.streamMode
                , headerSize
        );

        if( StreamMode.REQUEST == this.streamMode){

            HeaderField hostField = HeaderParser.findHeader(this.headerFieldList
                    , this.headerBuffer
                    , HttpHeader.HOST
            );

            this.host = hostField.value(this.headerBuffer);
            if( logger.isTraceEnabled() ) logger.trace("{} find-header, Host : {}", this.streamMode, this.host);

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
                if( logger.isTraceEnabled() ) logger.trace("{} Transfer-Encoding : {}", this.streamMode, transferEncodingVal);
            } else {
                if( logger.isTraceEnabled() ) logger.trace("{} Transfer-Encoding : NULL", this.streamMode);
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
                if( logger.isTraceEnabled() ) logger.trace("{} Content-Length : {}", this.streamMode, this.contentLength);
            } else {
                if( logger.isTraceEnabled() ) logger.trace("{} Content-Length : NULL", this.streamMode);
            }
        }

        if( logger.isTraceEnabled() ) logger.trace( "{} bodyMode : {}", this.streamMode, this.bodyMode);

        return headerBufferLen;
    }

    private int fillBodyBuffer() throws IOException {

        int remaining = this.bodyBuffer.length - this.bodyBufferLen;

        int read = in.read(this.bodyBuffer, this.bodyBufferLen, remaining);

        if( read < 0 ) {
            eof = true;
        } else {
            this.bodyBufferLen = this.bodyBufferLen + read;
        }

        if( logger.isTraceEnabled() ){
            logger.trace("{} fillBodyBuffer, read={}, bodyBufferLen={}"
                    , this.streamMode
                    , read
                    , this.bodyBufferLen
            );
            logger.trace("{} full body-buffer, length={}\n{}"
                    , this.streamMode
                    , this.bodyBufferLen
                    , ByteUtils.toHexPretty(this.bodyBuffer, 0, this.bodyBufferLen)
            );
        }

        return read;
    }

    public Chunk readChunk() throws IOException {

        if(logger.isTraceEnabled()) logger.trace( "{}, checkLength, bodyBufferLen={}"
                , this.streamMode
                , this.bodyBufferLen
        );

        if( this.bodyBufferLen < 20 ) {
            int read = fillBodyBuffer();
            if( read < 0 ){
                throw new IOException("error read chunk - eof");
            }
        }

        int idx = ByteUtils.indexOfCRLF(this.bodyBuffer, this.bodyBufferPos, this.bodyBufferLen);
        if( idx < 0 ){
            logger.error("{} invalid chunked format - {}"
                    , this.streamMode
                    , ByteUtils.toHexPretty(this.bodyBuffer, this.bodyBufferPos, this.headerBufferLen)
            );
            throw new IOException("Invalid chunked format");
        }

        Chunk chunk = new Chunk();
        chunk.start = this.bodyBufferPos;
        chunk.end = idx;
        chunk.size = ByteUtils.parseHex(this.bodyBuffer, this.bodyBufferPos, idx);

        return chunk;
    }

}
