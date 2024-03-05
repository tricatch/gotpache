package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cert.MultiDomainCertKeyManager;
import tricatch.gotpache.exception.ConfigException;
import tricatch.gotpache.server.AbstractServer;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.net.Socket;
import java.net.URL;
import java.security.KeyStore;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;

public class SSLPassServer extends AbstractServer {

    private static Logger logger = LoggerFactory.getLogger(tricatch.gotpache.pass.SSLPassServer.class);

    private int port;
    private int readTimeout;
    private int connectTimeout;
    private SSLContext sslContext;
    private PassHostMap virtualHosts;

    public SSLPassServer(Properties config) throws ConfigException {
        super(config);
    }

    @Override
    public void conifg() throws ConfigException {

        this.virtualHosts = new PassHostMap();
        this.port = this.getConfigAsInt( "ssl.pass.port", 443 );
        this.readTimeout = this.getConfigAsInt( "ssl.pass.read.timeout", 2000 );
        this.connectTimeout = this.getConfigAsInt( "ssl.pass.connect.timeout", 2000 );

        try {

        	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        	
            KeyManager[] kms = new KeyManager[]{ new MultiDomainCertKeyManager() };

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            
            TrustManager[] tms = tmf.getTrustManagers();

            this.sslContext = SSLContext.getInstance("TLS");
            this.sslContext.init(kms, tms, null);

            SSLEngine engine = this.sslContext.createSSLEngine();
            engine.setUseClientMode(false);


            //load virtual hosts
            Enumeration<?> cfgDomains = this.config.propertyNames();

            while(cfgDomains.hasMoreElements()) {

                String virtualHost = (String)cfgDomains.nextElement();
                PassHostList passHostList = new PassHostList();

                String strTargetHostInfo = config.getProperty(virtualHost);
                String[] targetHostInfos = strTargetHostInfo.split("\\s*,\\s*");

                logger.debug( "virtual-host-info, {} = {}", virtualHost, targetHostInfos );

                for(int i=0;i<targetHostInfos.length;i++){

                    URL url = null;
                    boolean ssl = false;

                    try{ url = new URL( targetHostInfos[i] ); }catch(Exception e){ continue; }

                    if( "https".equalsIgnoreCase( url.getProtocol() ) ) ssl = true;

                    int targetPort = url.getPort();

                    if( ssl && targetPort < 0 ) targetPort = 443;
                    if( !ssl && targetPort < 0 ) targetPort = 80;

                    PassHost passHost = new PassHost();
                    passHost.setVirtualHost(virtualHost);
                    passHost.setTargetHost( url.getHost() );
                    passHost.setTargetPort( targetPort );
                    passHost.setPath( url.getPath() );
                    passHost.setSsl( ssl );

                    passHostList.add( passHost );
                }

                if( passHostList.size() ==0 ) continue;

                class Descending implements Comparator<PassHost> {
                    @Override
                    public int compare(PassHost o1, PassHost o2) {
                        return o2.getPath().compareTo( o1.getPath() );
                    }
                }

                //longer url first
                Collections.sort(passHostList,new Descending());

                logger.debug( "virtual-host-added, {} = {}", virtualHost, passHostList );

                this.virtualHosts.put( virtualHost, passHostList );
            }

            logger.info( "virtual.hosts={}", virtualHosts );

        }catch(Exception e){
            e.printStackTrace();
            throw new ConfigException( "ssl config error - " + e.getMessage(), e );
        }
    }

    public void run() {

        try {

            logger.info("{} running...", this.getClass().getSimpleName());
            logger.info("{} port: {}", this.getClass().getSimpleName(), port);
            logger.info("{} client.connect.timeout: {}", this.getClass().getSimpleName(), connectTimeout );
            logger.info("{} client.read.timeout: {}", this.getClass().getSimpleName(), readTimeout );

            SSLServerSocketFactory ssf = this.sslContext.getServerSocketFactory();
            SSLServerSocket sslSvrSocket = (SSLServerSocket) ssf.createServerSocket(port);
            sslSvrSocket.setEnabledProtocols(new String[] { "TLSv1.3" });

            while (true) {

                Socket socket = sslSvrSocket.accept();

                if (socket == null) continue;

                if (logger.isTraceEnabled()) logger.trace("new pass - h{}", socket.hashCode());

                socket.setSoTimeout(this.readTimeout);

                ProxyPassServer.requestExecute(new PassExecutor(socket, this.virtualHosts, this.connectTimeout, this.readTimeout));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
