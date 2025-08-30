package tricatch.gotpache.server;

import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.exception.ConfigException;

public abstract class AbstractServer implements Runnable {

    protected Config config;
    protected VirtualHosts virtualHosts;

    public AbstractServer(Config config, VirtualHosts virtualHosts) throws ConfigException {
        this.config = config;
        this.virtualHosts = virtualHosts;
        this.conifg();
    }

    public abstract void conifg() throws ConfigException;

}
