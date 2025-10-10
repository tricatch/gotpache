package tricatch.gotpache.console.cmd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class CmdCaDownload implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdCaDownload.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        try {

            Config config = ProxyPassServer.getConfig();

            // Get CA certificate file path from config
            String certFileName = config.getCa().getCert();
            if (certFileName == null || certFileName.trim().isEmpty()) {
                logger.error("CA certificate file not configured");
                return createErrorResponse("CA certificate file not configured");
            }

            // Read the CA certificate file - try conf directory first
            Path certPath = Paths.get("conf", certFileName);
            
            // If file doesn't exist in conf directory, try other locations
            if (!Files.exists(certPath)) {
                // Try current working directory
                Path currentDirPath = Paths.get(System.getProperty("user.dir"), certFileName);
                if (Files.exists(currentDirPath)) {
                    certPath = currentDirPath;
                } else {
                    // Try direct path
                    Path directPath = Paths.get(certFileName);
                    if (Files.exists(directPath)) {
                        certPath = directPath;
                    } else {
                        logger.error("CA certificate file not found in any location: {}", certFileName);
                        return createErrorResponse(
                            "<!DOCTYPE html>" +
                            "<html><head><title>CA Certificate Not Found</title>" +
                            "<style>body{font-family:Arial,sans-serif;margin:40px;background:#f5f5f5;}" +
                            ".container{background:white;padding:30px;border-radius:8px;box-shadow:0 2px 10px rgba(0,0,0,0.1);}" +
                            ".error{color:#d32f2f;font-size:18px;margin-bottom:20px;}" +
                            ".info{color:#666;line-height:1.6;}" +
                            "</style></head>" +
                            "<body><div class='container'>" +
                            "<div class='error'>⚠️ CA Certificate Not Found</div>" +
                            "<div class='info'>" +
                            "<p>The CA certificate file <strong>" + certFileName + "</strong> could not be found.</p>" +
                            "<p>This usually means the CA certificate hasn't been generated yet. Please ensure:</p>" +
                            "<ul>" +
                            "<li>The Gotpache server has been started at least once</li>" +
                            "<li>SSL/TLS functionality has been initialized</li>" +
                            "<li>The CA certificate generation process has completed</li>" +
                            "</ul>" +
                            "<p>If you continue to see this error, please check the server logs for CA generation status.</p>" +
                            "</div></div></body></html>"
                        );
                    }
                }
            }

            byte[] certData = Files.readAllBytes(certPath);
            logger.info("CA certificate downloaded successfully: {} ({} bytes)", certFileName, certData.length);

            // Return file download response with original filename from config
            return ConsoleResponseBuilder.file(certData, certFileName);

        } catch (IOException e) {
            logger.error("Failed to read CA certificate file", e);
            return createErrorResponse("Failed to read CA certificate file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error during CA certificate download", e);
            return createErrorResponse("Unexpected error: " + e.getMessage());
        }
    }

    private ConsoleResponse createErrorResponse(String message) {
        try {
            return ConsoleResponseBuilder.ok(
                "<!DOCTYPE html>" +
                "<html><head><title>Error</title></head>" +
                "<body><h1>Error</h1><p>" + message + "</p></body></html>"
            );
        } catch (IOException e) {
            logger.error("Failed to create error response", e);
            return null;
        }
    }
}
