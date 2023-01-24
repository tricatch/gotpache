package tricatch.gotpache.pass;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpInputStream;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.InputStream;

public class PassClientRequest extends HttpInputStream {

    private String method = null;
    private String uri = null;
    private String host = null;

    public PassClientRequest(InputStream in) throws IOException {
        super(in);
        parseHost();
    }

    public String getMethod(){
        return this.method;
    }

    public String getUri() {
        return uri;
    }

    public String getHost() {
        return host;
    }

    private void parseHost() throws IOException {

        for(int i=0;i<this.headers.size();i++){

            byte[] bufLine = this.headers.get(i);
            String strLine = new String(bufLine).trim().toLowerCase();

            if( strLine.startsWith(HTTP.HEADER_HOST)){
                this.host = new String( ByteUtils.cut(bufLine, HTTP.HEADER_HOST.length()+1, bufLine.length) ).trim();
                break;
            }
        }

        byte[] line = this.headers.get(0);

        int pos1 = ByteUtils.indexOf( line, HTTP.SPACE );
        int pos2 = ByteUtils.lastIndexOf( line, HTTP.SPACE );

        byte[] bMethod = ByteUtils.cut(line, 0, pos1);
        byte[] bUrl = ByteUtils.cut(line,pos1+1, pos2);

        this.method = new String( bMethod );
        this.uri = new String( bUrl );

        if( "GET".equals(this.method) ){
            this.contentLength = 0;
        }
    }

}
