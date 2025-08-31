package tricatch.gotpache.exception;

public class ConfigException extends Exception {

	public ConfigException(String message, Exception e){
        super( message, e );
    }

    public ConfigException(String message){
        super(message);
    }
}
