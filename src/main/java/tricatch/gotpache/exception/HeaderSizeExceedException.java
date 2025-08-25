package tricatch.gotpache.exception;

import java.io.IOException;

public class HeaderSizeExceedException extends IOException {

    public HeaderSizeExceedException(String message){
        super(message);
    }
}
