package tricatch.gotpache.http;

public class HeaderField {

    public final int keyStart;
    public final int keyEnd;
    public final int valueStart;
    public final int valueEnd;

    public HeaderField(int keyStart
            , int keyEnd
            , int valueStart
            , int valueEnd
    ) {
        this.keyStart = keyStart;
        this.keyEnd = keyEnd;
        this.valueStart = valueStart;
        this.valueEnd = valueEnd;
    }

}
