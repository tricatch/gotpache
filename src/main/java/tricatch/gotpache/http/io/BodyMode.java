package tricatch.gotpache.http.io;

public enum BodyMode {
    NONE
    , CONTENT_LENGTH
    , CHUNKED
    , WEBSOCKET
    , UNTIL_CLOSE
    , NULL
}
