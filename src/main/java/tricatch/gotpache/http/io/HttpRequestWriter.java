package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.RequestHeader;
import tricatch.gotpache.http.ResponseHeader;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.OutputStream;

public class HttpRequestWriter {

    private Logger logger = logger = LoggerFactory.getLogger( HttpRequestWriter.class );

    private OutputStream out = null;
    public HttpRequestWriter(OutputStream out){
        this.out = out;
    }

    public void close() throws IOException {
        out.close();
    }

    public void flush() throws IOException {
        out.flush();
    }


    public void writeHeader(RequestHeader requestHeader) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write header, length={}, raw\n{}"
                    , requestHeader.raw.length
                    , ByteUtils.toHexPretty(requestHeader.raw, 0, requestHeader.raw.length)
            );
        }

        out.write(requestHeader.raw);
        out.flush();

    }

    public void writeBody(byte[] buf, int start, int end) throws IOException {

        if( logger.isTraceEnabled() ){
            logger.trace( "write body, length={}, raw\n{}"
                    , end - start
                    , ByteUtils.toHexPretty(buf, 0, end)
            );
        }

        out.write(buf, start, end);
        out.flush();
    }
}
