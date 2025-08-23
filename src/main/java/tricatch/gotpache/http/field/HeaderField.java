package tricatch.gotpache.http.field;

public class HeaderField {

    public final int keyStart, keyEnd;
    public final int valueStart, valueEnd;

    public HeaderField(int keyStart, int keyEnd, int valueStart, int valueEnd) {
        this.keyStart = keyStart;
        this.keyEnd = keyEnd;
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
    }

    public String key(byte[] buf) { return new String(buf, keyStart, keyEnd - keyStart); }
    public String value(byte[] buf) { return new String(buf, valueStart, valueEnd - valueStart); }
}
