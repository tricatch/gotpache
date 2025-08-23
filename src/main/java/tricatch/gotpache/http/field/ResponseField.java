package tricatch.gotpache.http.field;

public class ResponseField {

    public int versionStart, versionEnd;
    public int statusCodeStart, statusCodeEnd;
    public int reasonStart, reasonEnd;

    public String version(byte[] buf) {
        return new String(buf, versionStart, versionEnd - versionStart);
    }

    public String statusCode(byte[] buf) {
        return new String(buf, statusCodeStart, statusCodeEnd - statusCodeStart);
    }

    public String reason(byte[] buf) {
        if (reasonStart < 0) return null;
        return new String(buf, reasonStart, reasonEnd - reasonStart);
    }
}
