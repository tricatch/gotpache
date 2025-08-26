package tricatch.gotpache.http.io;

public enum Until {
    CRLF(2)
    , CRLFCRLF(4)
    ;

    final public int length;

    Until(int length){
        this.length = length;
    }
}
