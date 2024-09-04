package tricatch.gotpache.console;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpInputStream;
import tricatch.gotpache.util.ByteUtils;

import java.io.IOException;
import java.io.InputStream;

public class ConsoleRequest extends HttpInputStream {

    private String method = null;
    private String uri = null;

    public ConsoleRequest(InputStream in) throws IOException {
        super(in);
        preParse();
    }

    public String getUri() {
        return uri;
    }

    private void preParse() throws IOException {

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
