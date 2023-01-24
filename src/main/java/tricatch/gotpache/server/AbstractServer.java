package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.exception.ConfigException;

import java.util.Properties;

public abstract class AbstractServer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected Properties config = null;

    public AbstractServer(Properties config) throws ConfigException {
        this.config = config;
        this.conifg();
    }

    public abstract void conifg() throws ConfigException;

    public String getConfigAsString(String key, String defaultVal) throws ConfigException{

        if( this.config==null ) throw new ConfigException( "config is null" );

        String val = this.config.getProperty( key );

        if( logger.isDebugEnabled() ) logger.debug( "config {}={}[{}]", key, val, defaultVal );

        if( val==null || val.trim().length()==0 ) {
            if( defaultVal==null || defaultVal.trim().length()==0 ) throw new ConfigException( "not found config - " + key  );
            return defaultVal;
        }

        return val;
    }

    public int getConfigAsInt(String key, int defaultVal) throws ConfigException {

        if( this.config==null ) throw new ConfigException( "config is null" );

        String val = this.config.getProperty( key );

        if( logger.isDebugEnabled() ) logger.debug( "config {}={}[{}]", key, val, defaultVal );

        if( val==null || val.trim().length()==0 ) {
            return defaultVal;
        }

        try{

            return Integer.parseInt( val );

        }catch(Exception e){

            throw new ConfigException( "invalid config: " + key + "=" + val, e );
        }
    }
}
