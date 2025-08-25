package tricatch.gotpache.http;

public class ChunkStream {
    public byte[] buffer;
    public int start;
    public int end;
    public boolean last  = false;
}
