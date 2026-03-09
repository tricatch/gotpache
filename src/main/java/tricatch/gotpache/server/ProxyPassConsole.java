package tricatch.gotpache.server;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.cfg.attr.Console;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.cmd.CmdWelcome;
import tricatch.gotpache.console.cmd.CmdCaDownload;
import tricatch.gotpache.console.cmd.CmdCaGenerate;
import tricatch.gotpache.console.cmd.CmdCaCreate;
import tricatch.gotpache.console.cmd.CmdMonitor;
import tricatch.gotpache.console.cmd.CmdMonitorEvent;
import tricatch.gotpache.console.cmd.CmdProxyConfig;
import tricatch.gotpache.console.cmd.CmdProxyConfigsSave;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

public class ProxyPassConsole implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ProxyPassConsole.class);

    protected Map<String, ConsoleCommand> commands = new HashMap<>();

    public ProxyPassConsole() {

        commands.put("/", new CmdWelcome());
        commands.put("/welcome", new CmdWelcome());
        commands.put("/monitor", new CmdMonitor());
        commands.put("/monitor/event", new CmdMonitorEvent());
        commands.put("/proxyconfig", new CmdProxyConfig());
        commands.put("/proxyconfig/save", new CmdProxyConfigsSave());
        commands.put("/ca/download", new CmdCaDownload());
        commands.put("/ca/generate", new CmdCaGenerate());
        commands.put("/ca/create", new CmdCaCreate());
    }

    @Override
    public void run() {

        try {

            Thread.currentThread().setName("pt-console");

            Config config = ProxyPassServer.getConfig();

            String clazzName = this.getClass().getSimpleName();
            Console console = config.getConsole();

            logger.info("{} running...", clazzName);
            logger.info("{} port: {}", clazzName, console.getPort());

            // Create HttpServer
            HttpServer server = HttpServer.create(new InetSocketAddress(console.getPort()), 0);

            // Create HttpHandler for console commands
            HttpHandler consoleHandler = new ConsoleHttpHandler(commands, config);
            
            // Register handler for all paths
            server.createContext("/", consoleHandler);
            
            // Set executor to use virtual threads
            server.setExecutor(VThreadExecutor.getExecutor());
            
            // Start the server
            server.start();
            
            logger.info("{} HTTP server started on port {}", clazzName, console.getPort());

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }
}
