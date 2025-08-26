package tricatch.gotpache.http.io;

import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;

public class Reader {

    public int read(Until until, Read read, int shift) throws IOException {
        int start = buffer.idx;
        int end = start;

        for(;;){

            int remain = buffer.end - buffer.idx;

            if( remain < 0 ){
                throw new IllegalStateException("buffer index exceeds " + "(end=" + buffer.end + ", idx=" + buffer.idx + ")");
            }

            if( remain == 0 ){
                int emptyLen = buffer.raw.length - buffer.end;
                int n = this.in.read(buffer.raw, buffer.end, emptyLen);
                if( n < 0 ){
                    this.eof = true;
                    return n;
                }
                buffer.end += n;
            }

            int idx = switch (until) {
                case CRLF -> ByteUtils.indexOfCRLF(buffer.raw, buffer.idx, buffer.end);
                case CRLFCRLF -> ByteUtils.indexOfCRLFCRLF(buffer.raw, buffer.idx, buffer.end);
            };

            //not-found
            if( idx > 0 ){
                read.raw = buffer.raw;
                read.start = buffer.idx;
                read.end = idx + until.length;
                //remark
                buffer.idx = idx + until.length;
                return read.end - read.start;
            }

            buffer.idx = buffer.end;
        }
    }
}
