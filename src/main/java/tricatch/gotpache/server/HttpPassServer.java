package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Http;
import tricatch.gotpache.exception.ConfigException;
import tricatch.gotpache.pass.PassExecutor;

import java.net.ServerSocket;
import java.net.Socket;


public class HttpPassServer extends AbstractServer {

    private static final Logger logger = LoggerFactory.getLogger(HttpPassServer.class);

    public HttpPassServer(Config config, VirtualHosts virtualHosts) throws ConfigException {
        super(config, virtualHosts);
    }

    @Override
    public void conifg() {
        //nothing to do
    }

    public void run() {

        ServerSocket svrSocket = null;

        try {

            Thread.currentThread().setName("pt-http-pass");

            String clazzName = this.getClass().getSimpleName();
            Http http = this.config.getHttp();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, http.getPort());
            logger.info("{} client.connect.timeout: {}", clazzName, http.getConnectTimeout());
            logger.info("{} client.read.timeout: {}", clazzName, http.getReadTimeout());

            svrSocket = new ServerSocket(http.getPort());

            while (true) {

                Socket socket = svrSocket.accept();

                if (socket == null) continue;

                if (logger.isTraceEnabled()) logger.trace("new pass - h{}", socket.hashCode());

                socket.setSoTimeout(http.getConnectTimeout());

                VThreadExecutor.run(new PassExecutor(socket, this.virtualHosts, http.getConnectTimeout(), http.getReadTimeout()));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if( svrSocket!=null ) try{ svrSocket.close(); }catch (Exception e){}
        }
    }
}
