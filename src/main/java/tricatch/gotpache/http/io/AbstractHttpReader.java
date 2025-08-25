package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import tricatch.gotpache.exception.HeaderSizeExceedException;
import tricatch.gotpache.http.*;
import tricatch.gotpache.http.ref.DEF;
import tricatch.gotpache.util.ByteUtils;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

abstract class AbstractHttpReader {

    private Logger logger = null;

    protected InputStream in = null;
    protected boolean eof = false;

    protected byte[] buffer = new byte[DEF.MAX_BUFFER];
    protected int bufferPos = 0;
    protected int bufferEnd = 0;

    public AbstractHttpReader(InputStream in, Logger logger){
        this.in = in;
        this.logger = logger;
    }

    public void close() throws IOException {
        if( this.in !=null ) this.in.close();
    }

    public boolean EOF() {
        return eof;
    }

    public byte[] getBuffer(){
        return this.buffer;
    }

    public int getBufferPos(){
        return this.bufferPos;
    }

    public int getBufferEnd(){
        return this.bufferEnd;
    }

    public int fillBuffer(boolean moreRead) throws IOException {

        if( this.bufferEnd == 0 ){
            int read = this.in.read(this.buffer, 0, this.buffer.length);
            if( read < 0 ){
                eof = true;
                return read;
            }

            this.bufferEnd = read;

            if( logger.isTraceEnabled() ){
                logger.trace("fill-full, start=0, end={}", read);
            }

            return read;
        }

        if( moreRead ) {

            int remaining = this.buffer.length - this.bufferEnd;
            int read = this.in.read(this.buffer, this.bufferEnd, remaining);

            if (read < 0) {
                eof = true;
                return read;
            }

            this.bufferEnd += read;

            if (logger.isTraceEnabled()) {
                logger.trace("fill-more, start={}, end={}, add={}", this.bufferPos, this.bufferEnd, read);
            }

            return read;
        }

        return 0;
    }

    public void resetBufferIfEmpty(){

        if( logger.isTraceEnabled() ){
            logger.trace("remain-buffer, size={}", this.bufferEnd - this.bufferPos );
        }
        if( this.bufferEnd - this.bufferPos <= 0 ) {
            this.bufferPos = 0;
            this.bufferEnd = 0;

            if( logger.isTraceEnabled() ){
                logger.trace("reset-buffer / empty");
            }
        }
    }

    public void printBufferInfo(String tag, boolean raw){

        String rawData = "";

        if( raw ) rawData = "\n" + ByteUtils.toHexPretty(this.buffer, 0, this.bufferEnd);

        logger.debug( "buffer-{}, pos={}, end={}{}", tag, this.bufferPos, this.bufferEnd, rawData);
    }

    public byte[] copyAndShift(int len){

        //copy
        byte[] raw = new byte[len];
        System.arraycopy(this.buffer, 0, raw, 0, raw.length);

        //shift
        this.bufferPos = 0;
        this.bufferEnd = this.bufferEnd - len;
        System.arraycopy(this.buffer, len, this.buffer, 0, this.bufferEnd );

        logger.debug( "shift\n{}"
                , ByteUtils.toHexPretty(this.buffer, 0, this.bufferEnd)
        );

        return raw;
    }

    int readRawHeader() throws IOException {

        int lastPos = 0;
        int headerEnd = 0;

        //read-header-until-CRLFCRLF
        for (;;) {

            int read = this.fillBuffer(false);
            if (read < 0) throw new EOFException("Failed to read request headers");

            headerEnd = ByteUtils.indexOfCRLFCRLF(this.buffer, lastPos, this.bufferEnd);

            if( headerEnd > 0 ) {

                headerEnd += 4; //CRLFCRLF

                if( logger.isTraceEnabled() ){
                    logger.trace( "read-raw-header, length={}, raw\n{}"
                            , headerEnd
                            , ByteUtils.toHexPretty(this.buffer, 0, headerEnd)
                    );
                    logger.trace( "read-raw-header, length={}, text\n{}"
                            , headerEnd
                            , ByteUtils.toString(this.buffer, 0, headerEnd)
                    );
                }

                break;
            }

            //not-found-header
            if (this.bufferEnd >= this.buffer.length) {
                throw new HeaderSizeExceedException("Request header size exceeds the maximum allowed size of 8 KB");
            }
        }

        return headerEnd;
    }

    public ChunkSize readChunkSize() throws IOException {

        printBufferInfo("readChunkSize", true);

        int lineEnd = ByteUtils.indexOfCRLF(this.buffer, this.bufferPos, this.bufferEnd);
        if( lineEnd < 0 ){
            int more = fillBuffer(true);
            if( more < 0 ) return null;
            lineEnd = ByteUtils.indexOfCRLF(this.buffer, this.bufferPos, this.bufferEnd);
        }
        if( lineEnd < 0 ) throw new IllegalStateException("Chunk size block not found: missing CRLF");

        ChunkSize chunkSize = new ChunkSize();
        chunkSize.buffer = this.buffer;
        chunkSize.size = ByteUtils.hexToInt(this.buffer, this.bufferPos, lineEnd);
        chunkSize.start = this.bufferPos;
        chunkSize.end = lineEnd + 2; //CRLF

        //move pos
        this.bufferPos = chunkSize.end;

        if( logger.isTraceEnabled() ){
            logger.trace("read chunk-size={}", chunkSize.size);
        }

        printBufferInfo("readChunkSize", false);

        return chunkSize;
    }

    public ChunkStream readChunkStream(ChunkSize chunkSize) throws IOException {

        printBufferInfo( "readChunkStream",true);

        int remaining = this.bufferEnd - this.bufferPos;
        int readMore = chunkSize.size + 2 /*CRLF*/ - chunkSize.read;
        int read = Math.min(remaining, readMore);

        chunkSize.read += read;

        if(logger.isTraceEnabled()){
            logger.trace("read-chunk, size={}, totalRead={}, currentRead={}"
                    , chunkSize.size
                    , chunkSize.read
                    , read
                    );
        }

        ChunkStream chunkStream = new ChunkStream();
        chunkStream.buffer = this.buffer;
        chunkStream.start = this.bufferPos;
        chunkStream.end = this.bufferPos + read;
        chunkStream.last = chunkSize.size + 2 - chunkSize.read <= 0;

        //move pos
        this.bufferPos = this.bufferPos + read;

        printBufferInfo("readChunkStream", false);

        resetBufferIfEmpty();

        return chunkStream;
    }

    public ChunkEnd readChunkEnd() throws IOException {

        printBufferInfo("readChunkEnd", true);

        int start = this.bufferPos;

        for(;;) {

            int lineEnd = ByteUtils.indexOfCRLF(this.buffer, this.bufferPos, this.bufferEnd);
            if( lineEnd < 0 ){
                int more = fillBuffer(true);
                if( more < 0 ) return null;
                lineEnd = ByteUtils.indexOfCRLF(this.buffer, this.bufferPos, this.bufferEnd);
            }
            if( lineEnd < 0 ) throw new IllegalStateException("Chunk size block not found: missing CRLF");


            System.out.println( "end - " + lineEnd );

            if( lineEnd < 0 ){
                return null;
            }

            int dataLen = lineEnd - start;

            if (dataLen == 0) {
                ChunkEnd chunkEnd = new ChunkEnd();
                chunkEnd.buffer = this.buffer;
                chunkEnd.start = this.bufferPos;
                chunkEnd.end = lineEnd + 2;

                //move pos
                this.bufferPos = this.bufferPos + lineEnd + 2;

                return chunkEnd;
            }

            //Trailer header's info
            if (dataLen > 0) {
                start = lineEnd + 2;
            }
        }
    }
}
