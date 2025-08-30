package tricatch.gotpache.exception;

import java.io.IOException;

public class MaxBufferExceedException extends IOException {

    public MaxBufferExceedException(String message){
        super(message);
    }
}
