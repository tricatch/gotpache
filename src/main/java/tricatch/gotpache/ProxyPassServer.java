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
import tricatch.gotpache.server.HttpPassServer;
import tricatch.gotpache.server.ProxyPassConsole;
import tricatch.gotpache.server.SSLPassServer;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.util.JsonUtil;
import tricatch.gotpache.util.VirtualHostUtil;

import java.io.*;
import java.security.Security;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ProxyPassServer {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPassServer.class);

    private static final String CFG_FILE = "./conf/proxypass.yml";

    public static Config config = null;
    public static VirtualHosts virtualHosts = null;
    public static ThreadPoolExecutor serverExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void main(String[] args) {

        try{

            Representer representer = new Representer(new DumperOptions());
            representer.getPropertyUtils().setSkipMissingProperties(true);
            LoaderOptions loaderOptions = new LoaderOptions();

            Constructor constructor = new Constructor(Config.class, loaderOptions);
            Yaml yaml = new Yaml(constructor, representer);

            config = yaml.load(new FileInputStream(CFG_FILE));

            Security.addProvider(new BouncyCastleProvider());

            virtualHosts = VirtualHostUtil.convert( config.getVirtual(), config);

            if( virtualHosts.isEmpty() ){
                logger.error( "No virtual host" );
                return;
            }

            logger.info( "virtual.hosts={}", JsonUtil.pretty(virtualHosts) );

            serverExecutor.execute( new SSLPassServer( config, virtualHosts ) );
            serverExecutor.execute( new HttpPassServer( config, virtualHosts ) );
            serverExecutor.execute( new ProxyPassConsole( config ) );

        }catch(Exception e){
            logger.error( e.getMessage(), e );
        }

    }

}
