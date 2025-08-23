package tricatch.gotpache.http.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

public abstract class HttpInputStream {

    private static Logger logger = LoggerFactory.getLogger(HttpInputStream.class);

    protected InputStream in;
    protected List<byte[]> headers = new ArrayList<>();
    protected int contentLength = -1;
    protected boolean chunked = false;

    protected byte[] headerBuffer = new byte[HTTP.HEADER_BUFFER_SIZE];
    protected int endOfHeader = 0;
    protected int offset = 0;
    protected int idxHeader = 0;

    public HttpInputStream(InputStream in) throws IOException {

        this.in = in;

        readHeader();
        parseHeader();
    }

    public List<byte[]> getHeaders(){
        return this.headers;
    }

    public int getContentLength() {
        return contentLength;
    }

    public boolean isChunked() {
        return chunked;
    }

    public byte[] getHeaderBuffer() {
        return headerBuffer;
    }

    public int getEndOfHeader() {
        return endOfHeader;
    }

    public int getOffset() {
        return offset;
    }

    public InputStream getInputStream(){
        return this.in;
    }

    private void readHeader() throws IOException {

        int available = 0;
        int n = 0;

        for(;;){

            available = headerBuffer.length - offset;

            n = in.read( headerBuffer, offset, available );

            if( n<0 ){
                logger.error( "invalid header\n{}\n--end-of-header--", new String(this.headerBuffer, 0, this.offset) );
                throw new IOException( "header read error" );
            }

            endOfHeader = findHeaderEnd(headerBuffer, offset, offset+n);

            offset = offset + n;

            if( endOfHeader>0 ){

                idxHeader = endOfHeader;

                if( logger.isTraceEnabled() ){

                    logger.trace( "Active={}, HEADER\n{}", ProxyPassServer.requestActive(), new String(ByteUtils.cut(headerBuffer, 0, endOfHeader)) );

                    logger.trace( "read header - size: {}, offset: {}, remain={}, {}"
                            , endOfHeader, offset, offset-endOfHeader, ByteUtils.cut(headerBuffer, idxHeader, offset) );
                }


                break;
            }
        }
    }

    private int findHeaderEnd(final byte[] buf, int from, int to) {

        if( from>4 ) from = from - 4;

        for(int i=from;i<to;i++){

            // RFC2616
            if( from+4<to ){

                if( buf[i] == '\r' && buf[i+1] == '\n' && buf[i+2] == '\r' && buf[i+3] == '\n' ){
                    return i + 4;
                }
            }

            // tolerance
            if( from+2<4 ) {
                if (buf[i] == '\n' && buf[i+1] == '\n') {
                    return i + 2;
                }
            }
        }

        return -1;
    }



    private void parseHeader() throws IOException{

        int from = 0;

        for(int to=0;to<endOfHeader;to++){

            if( headerBuffer[to] == '\n' ){

                byte[] bufLine = new byte[to - from + 1];
                System.arraycopy( headerBuffer, from, bufLine,0, bufLine.length );
                from = to+1;
                this.headers.add(bufLine);
           }
        }

        for(int i=0;i<this.headers.size();i++){

            byte[] bufLine = this.headers.get(i);
            String strLine = new String(bufLine).trim().toLowerCase();

            //Content-Length:
            if(strLine.startsWith(HTTP.HEADER_CONTENT_LENGTH)){
                parseContentLength(bufLine);
                continue;
            }

            //Transfer-Encoding:
            if(strLine.startsWith(HTTP.HEADER_TRANSFER_ENCODING)){
                parseTransferEncoding(strLine);
                continue;
            }
        }
    }

    private void parseContentLength(byte[] buf){

        String line = new String(buf).trim();

        StringTokenizer st = new StringTokenizer(line);

        st.nextToken();

        String valCentLen = st.nextToken();

        this.contentLength = Integer.parseInt(valCentLen);
    }

    private void parseTransferEncoding(String strLine){

        if( strLine.indexOf(HTTP.TRENC_CHUNKED)>0 ){
            this.chunked = true;
        }

    }

    public int read() throws IOException {

        if( idxHeader < offset ){
            int b = this.headerBuffer[idxHeader];
            idxHeader = idxHeader + 1;

            if( logger.isTraceEnabled() ) logger.trace( "read()-buf - {}, [{}~{}={}]", b, offset, idxHeader, offset-idxHeader );

            return b;
        }

        int n = in.read();

        if( logger.isTraceEnabled() ) logger.trace( "read() - {}", n );

        return n;
    }

    public int read(byte[] buf) throws IOException {

        if( idxHeader < offset ){

            int remain = offset - idxHeader;

            if( remain > buf.length ){

                System.arraycopy( this.headerBuffer, idxHeader, buf, 0, buf.length );
                idxHeader = idxHeader + buf.length;

                if( logger.isTraceEnabled() ) logger.trace( "read(buf)-buf - {}, [{}~{}={}]", buf.length, offset, idxHeader, offset-idxHeader );

                return buf.length;

            } else {

                System.arraycopy( this.headerBuffer, idxHeader, buf, 0, remain );
                idxHeader = idxHeader + remain;

                if( logger.isTraceEnabled() ) logger.trace( "read(buf)-buf - {}, [{}~{}={}]", remain, offset, idxHeader, offset-idxHeader );

                return remain;
            }
        }

        int n = in.read(buf);

        if( logger.isTraceEnabled() ) logger.trace( "read(buf) - {}", n );

        return n;
    }

    public int read(byte[] buf, int off, int len) throws IOException {

        if( idxHeader < offset ){

            int remain = offset - idxHeader;

            if( remain > len ){

                System.arraycopy( this.headerBuffer, idxHeader, buf, 0, len );
                idxHeader = idxHeader + len;

                if( logger.isTraceEnabled() ) logger.trace( "read(...)-buf - {}, [{}~{}={}]", len, offset, idxHeader, offset-idxHeader );

                return len;

            } else {

                System.arraycopy( this.headerBuffer, idxHeader, buf, 0, remain );
                idxHeader = idxHeader + remain;

                if( logger.isTraceEnabled() ) logger.trace( "read(...)-buf -  {}, [{}~{}={}]", remain, offset, idxHeader, offset-idxHeader );

                return remain;
            }
        }


        int n = in.read(buf, off, len);

        if( logger.isTraceEnabled() ) logger.trace( "read(...) - {}", n );

        return n;
    }

    public int available() throws IOException {
        return in.available();
    }

    public void close(){

        if( this.in!=null ) try{ this.in.close(); }catch(Exception e){}
    }
}
