package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Https;
import tricatch.gotpache.exception.ConfigException;
import tricatch.gotpache.pass.PassRequestExecutor;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.Socket;

public class SSLPassServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SSLPassServer.class);

    public SSLPassServer() throws ConfigException {
    }

    public void run() {

        SSLServerSocket sslSvrSocket = null;
        Config config = ProxyPassServer.getConfig();

        try {

            Thread.currentThread().setName("pt-https-pass");

            String clazzName = this.getClass().getSimpleName();
            Https https = config.getHttps();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, https.getPort());
            logger.info("{} client.connect.timeout: {}", clazzName, https.getConnectTimeout() );
            logger.info("{} client.read.timeout: {}", clazzName, https.getReadTimeout() );

            SSLServerSocketFactory ssf = ProxyPassServer.getSslContext().getServerSocketFactory();
            sslSvrSocket = (SSLServerSocket) ssf.createServerSocket(https.getPort());
            sslSvrSocket.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.3" });

            while (true) {

                Socket socket = sslSvrSocket.accept();

                if (socket == null) continue;

                if (logger.isDebugEnabled()){
                    logger.debug("New client - h{}", socket.hashCode());
                }

                socket.setSoTimeout(https.getReadTimeout());

                VThreadExecutor.run(new PassRequestExecutor(socket, https.getConnectTimeout(), https.getReadTimeout()));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if( sslSvrSocket!=null ) try{ sslSvrSocket.close(); } catch (Exception e){}
        }
    }
}
