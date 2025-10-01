package tricatch.gotpache.console.cmd;

import freemarker.template.TemplateException;
import tricatch.gotpache.ProxyPassServer;
import tricatch.gotpache.util.FreeMarkerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;

public class CmdProxyConfig implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdProxyConfig.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        try {
            Config config = ProxyPassServer.getConfig();
            
            // Get client IP from request
            String clientIp = params.get("__clientIp");
            String fileName = "virtual-host-" + clientIp + ".yml";
            String confDir = "conf/vhost";
            Path confPath = Paths.get(confDir);
            Path targetFile = confPath.resolve(fileName);
            Path templateFile = confPath.resolve("virtual-host.yml");
            
            // Create conf directory if it doesn't exist
            if (!Files.exists(confPath)) {
                Files.createDirectories(confPath);
            }
            
            // If target file doesn't exist, copy from template
            if (!Files.exists(targetFile) && Files.exists(templateFile)) {
                Files.copy(templateFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
                logger.info("Created new virtual host file: {}", targetFile);
            }
            
            // Read file content
            String content = "";
            if (Files.exists(targetFile)) {
                content = new String(Files.readAllBytes(targetFile));
            }
            
            // Prepare data model
            Map<String, Object> dataModel = FreeMarkerUtil.createDataModel(config);
            dataModel.put("fileName", fileName);
            dataModel.put("fileContent", content);
            dataModel.put("clientIp", clientIp);
            
            // Render template
            String renderedContent = FreeMarkerUtil.renderTemplate("proxyconfig.ftl", dataModel);
            
            return ConsoleResponseBuilder.ok(renderedContent);

        } catch (IOException | TemplateException e) {
            logger.error("Failed to render settings page", e);
            try {
                return ConsoleResponseBuilder._404();
            } catch (IOException ioException) {
                logger.error("Failed to create error response", ioException);
                return null;
            }
        }
    }
}
