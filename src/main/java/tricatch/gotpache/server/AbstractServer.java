package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.exception.ConfigException;

public abstract class AbstractServer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    protected Config config = null;
    protected VirtualHosts virtualHosts;

    public AbstractServer(Config config, VirtualHosts virtualHosts) throws ConfigException {
        this.config = config;
        this.virtualHosts = virtualHosts;

        this.conifg();
    }

    public abstract void conifg() throws ConfigException;

}
