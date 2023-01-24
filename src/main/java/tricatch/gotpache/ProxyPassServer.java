package tricatch.gotpache;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.cert.CertTool;
import tricatch.gotpache.pass.HttpPassServer;
import tricatch.gotpache.pass.SSLPassServer;

import java.io.*;
import java.security.Security;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class ProxyPassServer {

    private static Logger logger = LoggerFactory.getLogger(ProxyPassServer.class);

    private static final String CFG_FILE = "./conf/proxypass.properties";

    public static Properties config = new Properties();

    public static final int MAX_THREAD = 100;

    public static ThreadPoolExecutor requestExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_THREAD);
    public static ThreadPoolExecutor serverExecutor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

    public static void requestExecute( Runnable runnable ){

        int active = requestExecutor.getActiveCount();

        if( active>=MAX_THREAD ){
            logger.warn( "No more thread - MAX={}", active);
        }

        requestExecutor.execute( runnable );
    }

    public static int requestActive(){
        return requestExecutor.getActiveCount();
    }

    public static void main(String[] args) {

        try{

    		Security.addProvider(new BouncyCastleProvider());
    		
        	CertTool.init();
        	
            config.load( new FileInputStream(CFG_FILE) );

            serverExecutor.execute( new SSLPassServer( config ) );
            serverExecutor.execute( new HttpPassServer( config ) );


        }catch(Exception e){

            logger.error( e.getMessage(), e );
        }

    }

}
