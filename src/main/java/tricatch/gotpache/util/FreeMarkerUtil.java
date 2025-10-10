package tricatch.gotpache.util;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import tricatch.gotpache.cfg.Config;

public class FreeMarkerUtil {

    private static final Configuration fmConfig;

    static {
        fmConfig = new Configuration(Configuration.VERSION_2_3_32);
        fmConfig.setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    /**
     * Render template with data model
     * @param templateName Template file name (e.g., "welcome.ftl")
     * @param dataModel Data model for template
     * @return Rendered HTML content
     * @throws IOException
     * @throws TemplateException
     */
    public static String renderTemplate(String templateName, Map<String, Object> dataModel) 
            throws IOException, TemplateException {
        
        // Create new configuration for each render to ensure fresh template loading
        Configuration config = new Configuration(Configuration.VERSION_2_3_32);
        config.setDefaultEncoding(StandardCharsets.UTF_8.name());
        
        // Try to use ClassTemplateLoader first (for JAR packaging), fallback to FileTemplateLoader
        try {
            // Use ClassTemplateLoader for JAR-compatible resource loading
            freemarker.cache.ClassTemplateLoader ctl = new freemarker.cache.ClassTemplateLoader(FreeMarkerUtil.class, "/templates");
            config.setTemplateLoader(ctl);
        } catch (Exception e) {
            // Fallback to FileTemplateLoader for development
            FileTemplateLoader ftl = new FileTemplateLoader(new File("src/main/resources/templates"));
            config.setTemplateLoader(ftl);
        }
        config.setTemplateUpdateDelayMilliseconds(0);
        
        Template template = config.getTemplate(templateName);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            template.process(dataModel, writer);
        }
        
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    /**
     * Get FreeMarker configuration instance
     * @return Configuration instance
     */
    public static Configuration getConfiguration() {
        return fmConfig;
    }

    /**
     * Create common data model with version and server name
     * @param config Configuration object
     * @return Data model map
     */
    public static Map<String, Object> createDataModel(Config config) {
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("version", config.getVersion());
        dataModel.put("serverName", config.getServerName());
        return dataModel;
    }
}
