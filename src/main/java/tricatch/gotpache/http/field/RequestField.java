package tricatch.gotpache.http.field;

public class RequestField {

    public int methodStart, methodEnd;
    public int pathStart, pathEnd;
    public int versionStart, versionEnd;

    public RequestField(){

    }

    public String method(byte[] buf) { return new String(buf, methodStart, methodEnd - methodStart); }
    public String path(byte[] buf) { return new String(buf, pathStart, pathEnd - pathStart); }
    public String version(byte[] buf) { return new String(buf, versionStart, versionEnd - versionStart); }
}
