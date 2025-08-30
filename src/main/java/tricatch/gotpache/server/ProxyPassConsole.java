package tricatch.gotpache.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Console;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleExecutor;
import tricatch.gotpache.console.cmd.CmdRootCa;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class ProxyPassConsole implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPassConsole.class);

    protected Config config;

    protected Map<String, ConsoleCommand> commands = new HashMap<>();

    public ProxyPassConsole(Config config) {

        this.config = config;

        commands.put(this.config.getCa().getDownload(), new CmdRootCa());
    }

    @Override
    public void run() {

        ServerSocket svrSocket = null;

        try {

            Thread.currentThread().setName("pt-console");

            String clazzName = this.getClass().getSimpleName();
            Console console = this.config.getConsole();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, console.getPort());

            svrSocket = new ServerSocket(console.getPort());

            while (true) {

                Socket socket = svrSocket.accept();

                if (socket == null) continue;

                if (logger.isTraceEnabled()) logger.trace("new pass - h{}", socket.hashCode());

                socket.setSoTimeout(console.getConnectTimeout());

                VThreadExecutor.run(new ConsoleExecutor(socket, commands, config));
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            if( svrSocket!=null ) try{ svrSocket.close(); } catch (Exception e){}
        }
    }
}
