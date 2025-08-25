package tricatch.gotpache.http;

import tricatch.gotpache.http.parser.HeaderParser;
import tricatch.gotpache.util.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class ResponseHeader {

    public byte[] raw;
    public int versionStart;
    public int versionEnd;
    public int statusCodeStart;
    public int statusCodeEnd;
    public int reasonStart;
    public int reasonEnd;

    private String version = null;
    private int status = 0;

    private String reason = null;

    public List<HeaderField> headers = new ArrayList<>();

    public String version(){
        if( version!=null ) return version;
        version = ByteUtils.toString(raw, versionStart, versionEnd);
        return version;
    }

    public int status(){
        if( status > 0 ) return status;
        status = ByteUtils.toInt(raw, statusCodeStart, statusCodeEnd);
        return status;
    }

    public String reason(){
        if( reason!=null ) return reason;
        reason = ByteUtils.toString(raw, reasonStart, reasonEnd);
        return reason;
    }
}
