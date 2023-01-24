package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.exception.ConfigException;
import tricatch.gotpache.server.AbstractServer;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;

public class HttpPassServer extends AbstractServer {

    private static Logger logger = LoggerFactory.getLogger(HttpPassServer.class);

    private int port;
    private int readTimeout;
    private int connectTimeout;
    private PassHostMap virtualHosts;
    private ServerSocket svrSocket = null;

    public HttpPassServer(Properties config) throws ConfigException {
        super(config);
    }

    @Override
    public void conifg() throws ConfigException {

        this.virtualHosts = new PassHostMap();
        this.port = this.getConfigAsInt( "pass.port", 80 );
        this.readTimeout = this.getConfigAsInt( "pass.read.timeout", 2000 );
        this.connectTimeout = this.getConfigAsInt( "pass.connect.timeout", 2000 );

        try {

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

                    try{ url = new URL( targetHostInfos[i] ); }catch(Exception e){ continue; }

                    int targetPort = url.getPort();

                    if( targetPort < 0 ) targetPort = 80;

                    PassHost passHost = new PassHost();
                    passHost.setVirtualHost(virtualHost);
                    passHost.setTargetHost( url.getHost() );
                    passHost.setTargetPort( targetPort );
                    passHost.setPath( url.getPath() );

                    passHostList.add( passHost );
                }

                if( passHostList.size() ==0 ) continue;

                class Descending implements Comparator<PassHost> {
                    @Override
                    public int compare(PassHost o1, PassHost o2) {
                        return o2.getPath().compareTo( o1.getPath() );
                    }
                }

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

            svrSocket = new ServerSocket(port);

            while (true) {

                Socket socket = svrSocket.accept();

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
