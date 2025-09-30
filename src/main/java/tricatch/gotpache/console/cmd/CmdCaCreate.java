package tricatch.gotpache.console.cmd;

import freemarker.template.TemplateException;
import io.github.tricatch.gotpache.cert.CertificateKeyPair;
import io.github.tricatch.gotpache.cert.KeyTool;
import io.github.tricatch.gotpache.cert.RootCertificateCreator;
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
import java.util.Map;
import java.util.LinkedHashMap;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.DumperOptions;

public class CmdCaCreate implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdCaCreate.class);
    
    /**
     * Update proxypass.yml file with new CA certificate information
     */
    private void updateProxypassYml(String caName, String certFileName, String keyFileName) {
        try {
            Path ymlPath = Paths.get("conf", "proxypass.yml");
            
            // Read existing YAML file
            Map<String, Object> yamlData = new LinkedHashMap<>();
            if (Files.exists(ymlPath)) {
                Yaml yaml = new Yaml();
                String content = Files.readString(ymlPath);
                if (!content.trim().isEmpty()) {
                    yamlData = yaml.load(content);
                }
            }
            
            // Ensure yamlData is not null
            if (yamlData == null) {
                yamlData = new LinkedHashMap<>();
            }
            
            // Update or create ca section
            Map<String, Object> caSection = new LinkedHashMap<>();
            caSection.put("cert", certFileName);
            caSection.put("priKey", keyFileName);
            caSection.put("priPwd", "");
            
            yamlData.put("ca", caSection);
            
            // Write back to file with proper formatting
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            
            Yaml yaml = new Yaml(options);
            String updatedContent = yaml.dump(yamlData);
            
            Files.writeString(ymlPath, updatedContent);
            logger.info("Updated proxypass.yml with new CA certificate: {}", certFileName);
            
        } catch (Exception e) {
            logger.error("Failed to update proxypass.yml", e);
        }
    }

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        String caName = params.get("caName");

        try {

            // Create conf directory if it doesn't exist
            Path confDir = Paths.get("conf");
            if (!Files.exists(confDir)) {
                Files.createDirectories(confDir);
            }

            // Generate CA certificate using gotpache-keytool
            RootCertificateCreator rootCertificateCreator = new RootCertificateCreator();

            // Generate root certificate
            CertificateKeyPair rootCert = rootCertificateCreator.generateRootCertificate(caName);

            // Create file names based on CA name
            String certFileName = caName + ".cer";
            String keyFileName = caName + ".pfx";

            // Save certificate files
            KeyTool keyTool = new KeyTool();
            keyTool.writeCertificate(rootCert.getCertificate(), "conf", certFileName);
            keyTool.writePrivateKey(rootCert.getPrivateKey(), "conf", keyFileName);

            logger.info("CA certificate generated successfully: {} for CA name: {}", certFileName, caName);

            // Update proxypass.yml with new CA certificate information
            updateProxypassYml(caName, certFileName, keyFileName);

            // init config & reload ssl-context
            try {
                logger.info("Reloading configuration and SSL context...");
                ProxyPassServer.initConfig();
                ProxyPassServer.initSslContext();
                logger.info("Configuration and SSL context reloaded successfully");
            } catch (Exception reloadException) {
                logger.error("Failed to reload configuration and SSL context", reloadException);
                // Continue with response even if reload fails
            }

            Config config = ProxyPassServer.getConfig();

            // Prepare data model using common utility
            Map<String, Object> dataModel = FreeMarkerUtil.createDataModel(config);
            dataModel.put("caCertFile", certFileName);
            dataModel.put("caKeyFile", keyFileName);
            dataModel.put("caName", caName);
            dataModel.put("success", true);

            // Render template using utility
            String content = FreeMarkerUtil.renderTemplate("ca-created.ftl", dataModel);

            return ConsoleResponseBuilder.ok(content);

        } catch (Exception e) {
            logger.error("Failed to generate CA certificate", e);
            
            try {

                Config config = ProxyPassServer.getConfig();

                // Prepare data model for error page using common utility
                Map<String, Object> dataModel = FreeMarkerUtil.createDataModel(config);
                dataModel.put("caName", caName);
                dataModel.put("success", false);
                dataModel.put("errorMessage", e.getMessage());

                // Render template using utility
                String content = FreeMarkerUtil.renderTemplate("ca-created.ftl", dataModel);

                return ConsoleResponseBuilder.ok(content);

            } catch (IOException | TemplateException templateException) {
                logger.error("Failed to render error page", templateException);
                try {
                    return ConsoleResponseBuilder._404();
                } catch (IOException ioException) {
                    logger.error("Failed to create error response", ioException);
                    return null;
                }
            }
        }
    }
}
