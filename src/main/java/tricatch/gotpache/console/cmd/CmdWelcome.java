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
import java.util.Map;

public class CmdWelcome implements ConsoleCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdWelcome.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) {

        try {

            Config config = ProxyPassServer.getConfig();

            // Prepare data model using common utility
            Map<String, Object> dataModel = FreeMarkerUtil.createDataModel(config);

            // Render template using utility
            String content = FreeMarkerUtil.renderTemplate("welcome.ftl", dataModel);

            return ConsoleResponseBuilder.ok(content);

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
