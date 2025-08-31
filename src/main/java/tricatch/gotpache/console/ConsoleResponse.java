package tricatch.gotpache.console;

import java.util.List;

public class ConsoleResponse {

    private List<String> headers;
    private byte[] body;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public byte[] getBody() {
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

}
