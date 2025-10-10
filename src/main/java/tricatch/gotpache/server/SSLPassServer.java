package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Https;
import tricatch.gotpache.pass.PassRequestExecutor;

import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import java.net.Socket;

public class SSLPassServer implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(SSLPassServer.class);

    private RunState runState = RunState.INIT;
    private boolean running = true;
    private SSLServerSocket sslSvrSocket = null;

    public SSLPassServer() {
    }

    public void stop(){
        this.running = false;
        if( this.sslSvrSocket!=null ) try{ this.sslSvrSocket.close(); }catch (Exception e){}
    }

    public RunState getRunState(){

        return this.runState;
    }

    public void run() {

        Config config = ProxyPassServer.getConfig();
        String clazzName = this.getClass().getSimpleName();

        try {

            Thread.currentThread().setName("pt-https-pass");

            Https https = config.getHttps();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, https.getPort());
            logger.info("{} client.connect.timeout: {}", clazzName, https.getConnectTimeout() );
            logger.info("{} client.read.timeout: {}", clazzName, https.getReadTimeout() );

            SSLServerSocketFactory ssf = ProxyPassServer.getSslContext().getServerSocketFactory();
            sslSvrSocket = (SSLServerSocket) ssf.createServerSocket(https.getPort());
            sslSvrSocket.setEnabledProtocols(new String[] { "TLSv1.2", "TLSv1.3" });

            this.running = true;
            this.runState = RunState.RUNNING;

            while (running) {

                Socket socket = sslSvrSocket.accept();

                if (socket == null) continue;

                if (logger.isDebugEnabled()){
                    logger.debug("New client - h{}", socket.hashCode());
                }

                socket.setSoTimeout(https.getReadTimeout());

                VThreadExecutor.run(new PassRequestExecutor(socket, https.getConnectTimeout(), https.getReadTimeout()));
            }

        } catch (Exception e) {
            if( "Socket closed".equals(e.getMessage()) ) logger.error( "sslPassServer socket closed");
            else logger.error("errorSslPassServer - " + e.getMessage(), e);
        } finally {
            if( sslSvrSocket!=null ) try{ sslSvrSocket.close(); } catch (Exception e){}
        }

        this.runState = RunState.STOPPED;
        logger.info("{} stopped...", clazzName);
    }
}
