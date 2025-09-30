package tricatch.gotpache;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.VirtualHost;
import tricatch.gotpache.cfg.VirtualHostsMap;
import tricatch.gotpache.exception.ConfigException;
import tricatch.gotpache.server.ProxyPassConsole;
import tricatch.gotpache.server.SSLPassServer;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.util.BrowserUtil;
import tricatch.gotpache.util.SSLUtil;
import tricatch.gotpache.util.VirtualHostUtil;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.MalformedURLException;
import java.security.Security;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ProxyPassServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPassServer.class);

    private static final String CFG_FILE = "./conf/proxypass.yml";
    private static final String VHOST_DIR = "./conf/vhost";

    public static ThreadPoolExecutor serverExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private static VirtualHostsMap virtualHostsMap = new VirtualHostsMap();
    private static Config config = null;
    private static SSLContext sslContext;

    public static void main(String[] args) {

        try {

            Security.addProvider(new BouncyCastleProvider());

            initConfig();

            try {
                initSslContext();
            } catch (ConfigException e) {
                logger.warn("SSL server initialization failed: {}. CA certificate may need to be generated.", e.getMessage());
                BrowserUtil.openUrl("http://127.0.0.1:" + config.getConsole().getPort() + "/ca/generate");
            }

            serverExecutor.execute(new SSLPassServer());
            serverExecutor.execute(new ProxyPassConsole());

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static void initConfig() throws FileNotFoundException {

        Representer representer = new Representer(new DumperOptions());
        representer.getPropertyUtils().setSkipMissingProperties(true);
        LoaderOptions loaderOptions = new LoaderOptions();

        Constructor constructorConfig = new Constructor(Config.class, loaderOptions);
        Yaml yamlConfig = new Yaml(constructorConfig, representer);

        config = yamlConfig.load(new FileInputStream(CFG_FILE));

        logger.info("Initialized config from {}", CFG_FILE);
    }

    public static void initSslContext() throws ConfigException {
        try {
            // Clear existing SSL context before initializing new one
            if (sslContext != null) {
                logger.info("Clearing existing SSL context");
            }
            
            sslContext = SSLUtil.initializeSSLContext(config);
            logger.info("SSL context initialized successfully");
            
        } catch (Exception e) {
            logger.error("Failed to initialize SSL context", e);
            sslContext = null; // Ensure sslContext is null on failure
            throw e;
        }
    }

    public static VirtualHosts getVirtualHosts(String clientId) throws FileNotFoundException, MalformedURLException {

        VirtualHosts virtualHosts = virtualHostsMap.get(clientId);

        if (virtualHosts != null) return virtualHosts;

        Representer representer = new Representer(new DumperOptions());
        representer.getPropertyUtils().setSkipMissingProperties(true);
        LoaderOptions loaderOptions = new LoaderOptions();

        Constructor constructorVirtualHost = new Constructor(VirtualHost.class, loaderOptions);
        Yaml yamlVirtualHost = new Yaml(constructorVirtualHost, representer);

        File vhFile = new File(VHOST_DIR + "/virtual-host.yml"); // default

        VirtualHost virtualHost = yamlVirtualHost.load(new FileInputStream(vhFile));

        virtualHosts = VirtualHostUtil.convert(virtualHost.getVirtual());

        virtualHostsMap.put(clientId, virtualHosts);

        return virtualHosts;
    }

    public static Config getConfig(){
        return config;
    }
    public static SSLContext getSslContext(){
        return sslContext;
    }
    
    /**
     * Restart SSL context by reinitializing it
     * @return true if restart was successful, false otherwise
     */
    public static boolean restartSslContext() {
        try {
            logger.info("Restarting SSL context...");
            initSslContext();
            logger.info("SSL context restarted successfully");
            return true;
        } catch (Exception e) {
            logger.error("Failed to restart SSL context", e);
            return false;
        }
    }
}
