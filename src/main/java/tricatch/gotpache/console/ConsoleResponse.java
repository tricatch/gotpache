package tricatch.gotpache.console;

import java.util.List;

public class ConsoleResponse {

    private List<String> headers;
    private byte[] response;

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public byte[] getResponse() {
        return response;
    }

    public void setResponse(byte[] response) {
        this.response = response;
    }

}
