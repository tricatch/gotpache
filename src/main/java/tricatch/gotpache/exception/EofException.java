package tricatch.gotpache.exception;

import java.io.IOException;

public class EofException extends IOException {

    public EofException(String message){
        super(message);
    }
}
