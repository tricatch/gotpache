package tricatch.gotpache.server;

import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.VirtualHostsMap;
import tricatch.gotpache.exception.ConfigException;

public abstract class AbstractServer implements Runnable {

    protected Config config;
    public AbstractServer(Config config) throws ConfigException {
        this.config = config;
        this.conifg();
    }

    public abstract void conifg() throws ConfigException;

}
