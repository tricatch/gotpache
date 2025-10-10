package tricatch.gotpache.console.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;
import tricatch.gotpache.server.VirtualHosts;
import tricatch.gotpache.util.JsonUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class CmdProxyConfigsSave implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdProxyConfigsSave.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        try {
            String clientIp = params.get("__clientIp");
            String fileName = "virtual-host-" + clientIp + ".yml";
            String content = params.get("content");

            String confDir = "conf/vhost";
            Path confPath = Paths.get(confDir);
            Path targetFile = confPath.resolve(fileName);
            
            // Create conf directory if it doesn't exist
            if (!Files.exists(confPath)) {
                Files.createDirectories(confPath);
            }
            
            // Write content to file
            Files.write(targetFile, content.getBytes());

            logger.info("Saved virtual host file: {}", targetFile);

            VirtualHosts virtualHosts = ProxyPassServer.getVirtualHosts(clientIp, true);
            if( logger.isDebugEnabled() ) logger.debug( "virtualHosts, client={}\n{}", clientIp, JsonUtil.pretty(virtualHosts));


            // Return JSON response
            String jsonResponse = "{\"success\": true, \"message\": \"File saved successfully\"}";
            return ConsoleResponseBuilder.ok(jsonResponse, "application/json");

        } catch (IOException e) {
            logger.error("Failed to save settings file", e);
            String jsonResponse = "{\"success\": false, \"message\": \"Failed to save file: " + e.getMessage() + "\"}";
            try {
                return ConsoleResponseBuilder.ok(jsonResponse, "application/json");
            } catch (IOException ioException) {
                logger.error("Failed to create error response", ioException);
                return null;
            }
        }
    }
}
