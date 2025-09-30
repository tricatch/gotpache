package tricatch.gotpache.console.cmd;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CmdWelcome implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdWelcome.class);

    @Override
    public ConsoleResponse execute(String uri, Config config) {

        try {
            // Create FreeMarker configuration
            Configuration fmConfig = new Configuration(Configuration.VERSION_2_3_32);
            fmConfig.setClassForTemplateLoading(this.getClass(), "/templates");
            fmConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());

            // Load template
            Template template = fmConfig.getTemplate("welcome.ftl");

            // Prepare data model
            Map<String, Object> dataModel = new HashMap<>();
            dataModel.put("version", "0.6.0");
            dataModel.put("serverName", "GotPache Console");

            // Process template
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
                template.process(dataModel, writer);
            }

            byte[] content = outputStream.toByteArray();

            return ConsoleResponseBuilder.ok(new String(content, StandardCharsets.UTF_8));

        } catch (IOException | TemplateException e) {
            logger.error("Failed to render welcome page", e);
            try {
                return ConsoleResponseBuilder._404();
            } catch (IOException ioException) {
                logger.error("Failed to create error response", ioException);
                return null;
            }
        }
    }
}
