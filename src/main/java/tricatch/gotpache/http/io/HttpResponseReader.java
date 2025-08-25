package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.BodyStream;
import tricatch.gotpache.http.ref.DEF;
import tricatch.gotpache.http.ResponseHeader;
import tricatch.gotpache.http.parser.HeaderParser;
import tricatch.gotpache.http.ref.StreamType;

import java.io.IOException;
import java.io.InputStream;

public class HttpResponseReader extends AbstractHttpReader {

    private static Logger logger = LoggerFactory.getLogger(HttpResponseReader.class);

    public HttpResponseReader(InputStream in) {
        super(in, logger);
    }

    public ResponseHeader readHeader() throws IOException {

        ResponseHeader responseHeader = new ResponseHeader();

        int headerEnd = this.readRawHeader();

        responseHeader.raw = this.copyAndShift(headerEnd);

        int headerStart = HeaderParser.parseResponseLine(
                responseHeader.raw
                , 0
                , responseHeader.raw.length
                , responseHeader
        );

        HeaderParser.parseHeader(
                responseHeader.raw
                , headerStart
                , responseHeader.raw.length
                , responseHeader.headers
        );

        return responseHeader;
    }

    public BodyStream getBodyStream(ResponseHeader responseHeader) {

        BodyStream bodyStream = new BodyStream();

        try {

            bodyStream.connection = HeaderParser.valueAsString(
                    responseHeader.headers
                    , responseHeader.raw
                    , DEF.HEADER.CONNECTION
            );

            bodyStream.keepAlive = HeaderParser.valueAsString(
                    responseHeader.headers
                    , responseHeader.raw
                    , DEF.HEADER.KEEP_ALIVE
            );


            //status
            int status = responseHeader.status();
            if ((status >= 100 && status < 200)
                    || status == 204
                    || status == 304
            ) {
                bodyStream.streamType = StreamType.NONE;
                return bodyStream;
            }

            //transfer-encoding
            String transferEncoding = HeaderParser.valueAsString(
                    responseHeader.headers
                    , responseHeader.raw
                    , DEF.HEADER.TRANSFER_ENCODING
            );

            if ("chunked".equals(transferEncoding)) {
                bodyStream.streamType = StreamType.CHUNKED;
                return bodyStream;
            }

            //content-length
            int contentLength = HeaderParser.valueAsInt(
                    responseHeader.headers
                    , responseHeader.raw
                    , DEF.HEADER.CONTENT_LENGTH
            );

            if (contentLength > 0) {
                bodyStream.streamType = StreamType.CONTENT_LENGTH;
                bodyStream.contentLength = contentLength;
                return bodyStream;
            }

            return bodyStream;

        } finally {

            if (logger.isTraceEnabled()) logger.trace("body-Stream : {}, content-length={}, connection={}, keep-alive={}"
                    , bodyStream.streamType
                    , bodyStream.contentLength
                    , bodyStream.connection
                    , bodyStream.keepAlive
            );
        }

    }



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
