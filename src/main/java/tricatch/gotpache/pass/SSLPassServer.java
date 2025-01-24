package tricatch.gotpache.pass;

import io.github.tricatch.gotpache.cert.KeyTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cert.MultiDomainCertKeyManager;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Ca;
import tricatch.gotpache.cfg.attr.Https;
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
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class SSLPassServer extends AbstractServer {

    private static Logger logger = LoggerFactory.getLogger(tricatch.gotpache.pass.SSLPassServer.class);

    private SSLContext sslContext;


    public SSLPassServer(Config config, VirtualHosts virtualHosts) throws ConfigException {
        super(config, virtualHosts);
    }

    @Override
    public void conifg() throws ConfigException {

        try {

            Ca ca = config.getCa();

            KeyTool keyTool = new KeyTool();
            X509Certificate rootCertificate = keyTool.readCertificate("./conf", ca.getCert());
            PrivateKey rootPrivateKey = null;

            if( config.getCa().getPriPwd()!=null && !ca.getPriPwd().trim().isEmpty() ){
                rootPrivateKey = keyTool.readPrivateKey("./conf", ca.getPriKey(), ca.getPriPwd().trim());
            } else {
                rootPrivateKey = keyTool.readPrivateKey("./conf", ca.getPriKey());
            }

        	KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        	
            KeyManager[] kms = new KeyManager[]{
                    new MultiDomainCertKeyManager(rootCertificate, rootPrivateKey)
            };

            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(ks);
            
            TrustManager[] tms = tmf.getTrustManagers();

            this.sslContext = SSLContext.getInstance("TLS");
            this.sslContext.init(kms, tms, null);

            SSLEngine engine = this.sslContext.createSSLEngine();
            engine.setUseClientMode(false);

        }catch(Exception e){
            e.printStackTrace();
            throw new ConfigException( "ssl config error - " + e.getMessage(), e );
        }
    }

    public void run() {

        try {

            String clazzName = this.getClass().getSimpleName();
            Https https = this.config.getHttps();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, https.getPort());
            logger.info("{} client.connect.timeout: {}", clazzName, https.getConnectTimeout() );
            logger.info("{} client.read.timeout: {}", clazzName, https.getReadTimeout() );

            SSLServerSocketFactory ssf = this.sslContext.getServerSocketFactory();
            SSLServerSocket sslSvrSocket = (SSLServerSocket) ssf.createServerSocket(https.getPort());
            sslSvrSocket.setEnabledProtocols(new String[] { "TLSv1.3" });

            while (true) {

                Socket socket = sslSvrSocket.accept();

                if (socket == null) continue;

                if (logger.isTraceEnabled()) logger.trace("new pass - h{}", socket.hashCode());

                socket.setSoTimeout(https.getReadTimeout());

                ProxyPassServer.requestExecute(new PassExecutor(socket, this.virtualHosts, https.getConnectTimeout(), https.getReadTimeout()));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }
}
