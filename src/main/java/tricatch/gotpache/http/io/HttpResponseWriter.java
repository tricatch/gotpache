package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.*;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;

public class HttpResponseWriter {

    private Logger logger = LoggerFactory.getLogger(HttpResponseWriter.class);

    private OutputStream out = null;


    public HttpResponseWriter(OutputStream out){
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void flush() throws IOException {
        out.flush();
    }

    private void writeAndFlush(byte[] buf, int start, int end) throws IOException {

        out.write(buf, start, (end-start) );
        out.flush();
    }

    public void writeHeader(ResponseHeader responseHeader) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write header, length={}, raw\n{}"
                    , responseHeader.raw.length
                    , ByteUtils.toHexPretty(responseHeader.raw, 0, responseHeader.raw.length)
            );
        }

        writeAndFlush(responseHeader.raw, 0, responseHeader.raw.length);
    }

    public void writeChunkSize(ChunkSize chunkSize) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-size, len={}, raw\n{}"
                    , chunkSize.end - chunkSize.start
                    , ByteUtils.toHexPretty(chunkSize.buffer, chunkSize.start, chunkSize.end)
            );
        }

        writeAndFlush(chunkSize.buffer, chunkSize.start, chunkSize.end);
    }

    public void writeChunkStream(ChunkStream chunkStream) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-stream, len={}, raw\n{}"
                    , chunkStream.end - chunkStream.start
                    , ByteUtils.toHexPretty(chunkStream.buffer, chunkStream.start, chunkStream.end)
            );
        }

        writeAndFlush(chunkStream.buffer, chunkStream.start, chunkStream.end);
    }

    public void writeChunkEnd(ChunkEnd chunkEnd) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-end, len={}, raw\n{}"
                    , chunkEnd.end - chunkEnd.start
                    , ByteUtils.toHexPretty(chunkEnd.buffer, chunkEnd.start, chunkEnd.end)
            );
        }

        writeAndFlush(chunkEnd.buffer, chunkEnd.start, chunkEnd.end);
    }

}
