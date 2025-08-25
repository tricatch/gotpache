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


    public void writeHeader(ResponseHeader responseHeader) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write header, length={}, raw\n{}"
                    , responseHeader.raw.length
                    , ByteUtils.toHexPretty(responseHeader.raw, 0, responseHeader.raw.length)
            );
        }

        out.write(responseHeader.raw);
        out.flush();

    }

    public void writeChunkSize(ChunkSize chunkSize) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-size, len={}, raw\n{}"
                    , chunkSize.end - chunkSize.start
                    , ByteUtils.toHexPretty(chunkSize.buffer, chunkSize.start, chunkSize.end)
            );
        }

        out.write(chunkSize.buffer, chunkSize.start, chunkSize.end);
        out.flush();
    }

    public void writeChunkStream(ChunkStream chunkStream) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-stream, len={}, raw\n{}"
                    , chunkStream.end - chunkStream.start
                    , ByteUtils.toHexPretty(chunkStream.buffer, chunkStream.start, chunkStream.end)
            );
        }

        out.write(chunkStream.buffer, chunkStream.start, chunkStream.end);
        out.flush();
    }

    public void writeChunkEnd(ChunkEnd chunkEnd) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write chunk-end, len={}, raw\n{}"
                    , chunkEnd.end - chunkEnd.start
                    , ByteUtils.toHexPretty(chunkEnd.buffer, chunkEnd.start, chunkEnd.end)
            );
        }

        out.write(chunkEnd.buffer, chunkEnd.start, chunkEnd.end);
        out.flush();
    }

}
