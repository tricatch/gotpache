package tricatch.gotpache.http;

import tricatch.gotpache.http.parser.HeaderParser;
import tricatch.gotpache.http.ref.DEF;
import tricatch.gotpache.util.ByteUtils;

import java.util.ArrayList;
import java.util.List;

public class RequestHeader {

    public byte[] raw;
    public int methodStart;
    public int methodEnd;
    public int pathStart;
    public int pathEnd;
    public int versionStart;
    public int versionEnd;

    private String method = null;
    private String path = null;
    private String version = null;
    private String host = null;

    public List<HeaderField> headers = new ArrayList<>();

    public String method(){
        if( method!=null ) return method;
        method = ByteUtils.toString(raw, methodStart, methodEnd);
        return method;
    }

    public String path(){
        if( path!=null ) return path;
        path = ByteUtils.toString(raw, pathStart, pathEnd);
        return path;
    }

    public String version(){
        if( version!=null ) return version;
        version = ByteUtils.toString(raw, versionStart, versionEnd);
        return version;
    }

    public String host(){
        if( host!=null ) return host;
        host = HeaderParser.valueAsString(headers, raw, DEF.HEADER.HOST);
        return host;
    }
}
