package tricatch.gotpache.util;

import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
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
        
        // Development: use FileTemplateLoader so FTL changes load immediately (no server restart)
        File templatesDir = new File("src/main/resources/templates");
        if (templatesDir.exists() && templatesDir.isDirectory()) {
            config.setTemplateLoader(new FileTemplateLoader(templatesDir));
            config.setTemplateUpdateDelayMilliseconds(0); // always re-read from disk
        } else {
            // JAR/production: use classpath
            config.setTemplateLoader(new freemarker.cache.ClassTemplateLoader(FreeMarkerUtil.class, "/templates"));
        }
        
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
     * Load version from version.properties (built from build.gradle)
     */
    private static String loadVersionFromProperties() {
        try (InputStream is = FreeMarkerUtil.class.getResourceAsStream("/version.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String v = props.getProperty("version");
                if (v != null && !v.isEmpty() && !v.contains("${")) return v;
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    /**
     * Create common data model with version and server name
     * Version is read from version.properties (build.gradle) with fallback to config
     * @param config Configuration object
     * @return Data model map
     */
    public static Map<String, Object> createDataModel(Config config) {
        Map<String, Object> dataModel = new HashMap<>();
        String version = loadVersionFromProperties();
        dataModel.put("version", version);
        dataModel.put("serverName", config.getServerName());
        return dataModel;
    }
}
