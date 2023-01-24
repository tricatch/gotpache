package tricatch.gotpache.http.io;

import java.io.IOException;
import java.io.InputStream;

public class ServerResponse extends HttpInputStream {

    public ServerResponse(InputStream in) throws IOException {
        super(in);
    }
}
