package tricatch.gotpache.http;

import tricatch.gotpache.http.ref.StreamType;

public class BodyStream {
    public StreamType streamType = StreamType.NULL;
    public int contentLength = -1;
    public String keepAlive = null;
    public String connection = null;
}
