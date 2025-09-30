package tricatch.gotpache.console.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class CmdSettingsSave implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdSettingsSave.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        try {
            String fileName = params.get("fileName");
            String content = params.get("content");
            String clientIp = params.get("clientIp");
            
            if (fileName == null || content == null) {
                return ConsoleResponseBuilder.error("Missing fileName or content parameter");
            }
            
            // Security check: ensure the file name matches the client IP
            if (!fileName.startsWith("virtual-host-") || !fileName.endsWith(".yml")) {
                return ConsoleResponseBuilder.error("Invalid file name format");
            }
            
            // Extract IP from filename and verify it matches client IP
            String fileIp = fileName.substring("virtual-host-".length(), fileName.length() - ".yml".length());
            if (!fileIp.equals(clientIp)) {
                return ConsoleResponseBuilder.error("Access denied: Cannot modify other IP's configuration");
            }
            
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
