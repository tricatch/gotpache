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
import tricatch.gotpache.server.ProxyPassConsole;
import tricatch.gotpache.server.SSLPassServer;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.util.VirtualHostUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.security.Security;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ProxyPassServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPassServer.class);

    private static final String CFG_FILE = "./conf/proxypass.yml";
    private static final String VHOST_DIR = "./conf/vhost";

    public static Config config = null;
    public static ThreadPoolExecutor serverExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    private static VirtualHostsMap virtualHostsMap = new VirtualHostsMap();

    public static void main(String[] args) {

        try{
            Security.addProvider(new BouncyCastleProvider());

            Representer representer = new Representer(new DumperOptions());
            representer.getPropertyUtils().setSkipMissingProperties(true);
            LoaderOptions loaderOptions = new LoaderOptions();

            Constructor constructorConfig = new Constructor(Config.class, loaderOptions);
            Yaml yamlConfig = new Yaml(constructorConfig, representer);

            config = yamlConfig.load(new FileInputStream(CFG_FILE));

            serverExecutor.execute( new SSLPassServer( config ) );
            serverExecutor.execute( new ProxyPassConsole( config ) );

        }catch(Exception e){
            logger.error( e.getMessage(), e );
        }

    }

    public static VirtualHosts getVirtualHosts(String clientId) throws FileNotFoundException, MalformedURLException {

        VirtualHosts virtualHosts = virtualHostsMap.get(clientId);

        if( virtualHosts!=null ) return virtualHosts;

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
}
