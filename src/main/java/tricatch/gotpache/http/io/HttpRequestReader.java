package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.RequestHeader;
import tricatch.gotpache.http.parser.HeaderParser;
import java.io.IOException;
import java.io.InputStream;

public class HttpRequestReader extends AbstractHttpReader {

    private static Logger logger = LoggerFactory.getLogger( HttpRequestReader.class );

    public HttpRequestReader(InputStream in){
        super(in, logger);
    }

    public RequestHeader readHeader() throws IOException {

        RequestHeader requestHeader = new RequestHeader();

        int headerEnd = this.readRawHeader();

        requestHeader.raw = this.copyAndShift(headerEnd);

        int headerStart = HeaderParser.parseRequestLine(
                requestHeader.raw
                , 0
                , requestHeader.raw.length
                , requestHeader
        );

        HeaderParser.parseHeader(
                requestHeader.raw
                , headerStart
                , requestHeader.raw.length
                , requestHeader.headers
        );

        return requestHeader;
    }

//    public BodyStream getBodyStream(RequestHeader requestHeader){
//
//    }


//        //parse-header
//        int headerStart = 0;
//
//        if( ReaderMode.REQUEST == readerMode){
//
//            headerStart = HeaderParser.parseRequestLine(this.headerBuffer, 0, headerBufferLen, this.requestHeader);
//
//            this.method = this.requestHeader.method(this.headerBuffer);
//            this.path = this.requestHeader.path(this.headerBuffer);
//            this.version = this.requestHeader.version(this.headerBuffer);
//
//            if( logger.isDebugEnabled() ) logger.trace( "{} parsedRequestLine ( {}, {}, {} )"
//                    , this.readerMode
//                    , this.method
//                    , this.path
//                    , this.version
//            );
//
//        } else {
//
//            headerStart = HeaderParser.parseStatusLine(this.headerBuffer, 0, headerBufferLen, this.responseHeader);
//
//            this.version = this.responseHeader.version(this.headerBuffer);
//            this.status = Integer.parseInt(this.responseHeader.statusCode(this.headerBuffer));
//            this.reason = this.responseHeader.reason(this.headerBuffer);
//
//            if( logger.isDebugEnabled() ) logger.trace( "{} parsedResponseLine ( {}, {}, {} )"
//                    , this.readerMode
//                    , this.version
//                    , this.status
//                    , this.reason
//            );
//        }
//
//        int headerSize = HeaderParser.parseHeader(this.headerBuffer, headerStart, headerBufferLen, this.headerFieldList);
//
//        if( logger.isDebugEnabled() ) logger.trace("{} parsedHeaderSize {}"
//                , this.readerMode
//                , headerSize
//        );
//
//        if( ReaderMode.REQUEST == this.readerMode){
//
//            HeaderField hostField = HeaderParser.findHeader(this.headerFieldList
//                    , this.headerBuffer
//                    , DEF.HEADER.HOST
//            );
//
//            this.host = hostField.value(this.headerBuffer);
//            if( logger.isTraceEnabled() ) logger.trace("{} find-header, Host : {}", this.readerMode, this.host);
//
//            switch (this.method ){
//                case "GET" :
//                case "HEAD" :
//                case "OPTIONS" :
//                case "DELETE" :
//                case "TRACE" :
//                    this.bodyStream = BodyStream.NONE;
//                    break;
//            }
//
//        } else {
//
//            if ((this.status >= 100 && this.status < 200)
//                    || this.status == 204
//                    || this.status == 304) {
//
//                this.bodyStream = BodyStream.NONE;
//            }
//
//        }
//
//        //chunked
//        if( BodyStream.NULL == this.bodyStream){
//            HeaderField transferEncodingField = HeaderParser.findHeader(this.headerFieldList
//                    , this.headerBuffer
//                    , DEF.HEADER.TRANSFER_ENCODING
//            );
//            if( transferEncodingField!=null ){
//                String transferEncodingVal = transferEncodingField.value(this.headerBuffer);
//                if( "chunked".equals(transferEncodingVal) ) this.bodyStream = BodyStream.CHUNKED;
//                if( logger.isTraceEnabled() ) logger.trace("{} Transfer-Encoding : {}", this.readerMode, transferEncodingVal);
//            } else {
//                if( logger.isTraceEnabled() ) logger.trace("{} Transfer-Encoding : NULL", this.readerMode);
//            }
//        }
//
//        //content-length
//        if( BodyStream.NULL == this.bodyStream) {
//            HeaderField contentLengthField = HeaderParser.findHeader(this.headerFieldList
//                    , this.headerBuffer
//                    , DEF.HEADER.CONTENT_LENGTH
//            );
//            if( contentLengthField!=null ){
//                this.contentLength = Integer.parseInt(contentLengthField.value(this.headerBuffer));
//                this.bodyStream = BodyStream.CONTENT_LENGTH;
//                if( logger.isTraceEnabled() ) logger.trace("{} Content-Length : {}", this.readerMode, this.contentLength);
//            } else {
//                if( logger.isTraceEnabled() ) logger.trace("{} Content-Length : NULL", this.readerMode);
//            }
//        }
//
//        if( logger.isTraceEnabled() ) logger.trace( "{} bodyMode : {}", this.readerMode, this.bodyStream);
//
//        return headerBufferLen;
//    }
//
//    private int fillBodyBuffer() throws IOException {
//
//        int remaining = this.bodyBuffer.length - this.bodyBufferLen;
//
//        int read = in.read(this.bodyBuffer, this.bodyBufferLen, remaining);
//
//        if( read < 0 ) {
//            eof = true;
//        } else {
//            this.bodyBufferLen = this.bodyBufferLen + read;
//        }
//
//        if( logger.isTraceEnabled() ){
//            logger.trace("{} fillBodyBuffer, read={}, bodyBufferLen={}"
//                    , this.readerMode
//                    , read
//                    , this.bodyBufferLen
//            );
//            logger.trace("{} full body-buffer, length={}\n{}"
//                    , this.readerMode
//                    , this.bodyBufferLen
//                    , ByteUtils.toHexPretty(this.bodyBuffer, 0, this.bodyBufferLen)
//            );
//        }
//
//        return read;
//    }
//
//    public Chunk readChunk() throws IOException {
//
//        if(logger.isTraceEnabled()) logger.trace( "{}, checkLength, bodyBufferLen={}"
//                , this.readerMode
//                , this.bodyBufferLen
//        );
//
//        if( this.bodyBufferLen < 20 ) {
//            int read = fillBodyBuffer();
//            if( read < 0 ){
//                throw new IOException("error read chunk - eof");
//            }
//        }
//
//        int idx = ByteUtils.indexOfCRLF(this.bodyBuffer, this.bodyBufferPos, this.bodyBufferLen);
//        if( idx < 0 ){
//            logger.error("{} invalid chunked format - {}"
//                    , this.readerMode
//                    , ByteUtils.toHexPretty(this.bodyBuffer, this.bodyBufferPos, this.headerBufferLen)
//            );
//            throw new IOException("Invalid chunked format");
//        }
//
//        Chunk chunk = new Chunk();
//        chunk.start = this.bodyBufferPos;
//        chunk.end = idx;
//        chunk.size = ByteUtils.parseHex(this.bodyBuffer, this.bodyBufferPos, idx);
//
//        return chunk;
//    }

}
