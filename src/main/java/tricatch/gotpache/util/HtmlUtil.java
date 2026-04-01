package tricatch.gotpache.util;

import freemarker.template.TemplateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.exception.BadGatewayException;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpStreamWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HtmlUtil {

    private static final Logger logger = LoggerFactory.getLogger(HtmlUtil.class);

    public static String escapeHtmlPlain(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    public static void writeBadGatewayResponse(HttpStreamWriter out, BadGatewayException ex) throws IOException {
        String html;
        try {
            Map<String, Object> model = new HashMap<>();
            model.put("rid", ex.getRid());
            model.put("requestHost", ex.getRequestHost());
            model.put("routePath", ex.getRoutePath());
            model.put("targetUrl", ex.getTargetUrl());
            Throwable cause = ex.getCause();
            model.put("errorMessage", cause != null ? cause.getMessage() : null);
            html = FreeMarkerUtil.renderTemplate("bad-gateway.ftl", model);
        } catch (IOException | TemplateException e) {
            logger.error("{}, Failed to render bad-gateway.ftl: {}", ex.getRid(), e.getMessage(), e);
            html = "<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"utf-8\"><title>502 Bad Gateway</title></head><body>"
                    + "<p>Gotpache</p><h1>502 Bad Gateway</h1><p>The proxy could not connect to the upstream server.</p>"
                    + "<p>Route path: " + escapeHtmlPlain(ex.getRoutePath()) + "</p>"
                    + "<p>Target URL: " + escapeHtmlPlain(ex.getTargetUrl()) + "</p></body></html>";
        }

        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        out.write("HTTP/1.1 502 Bad Gateway\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Content-Type: text/html; charset=utf-8\r\n".getBytes(StandardCharsets.UTF_8));
        out.write("Connection: close\r\n".getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Length: " + body.length + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(HTTP.CRLF);
        out.write(body);
        out.flush();
    }

    public static byte[] toBytes(List<String> list) throws IOException {

        ByteArrayOutputStream buf = new ByteArrayOutputStream(1024);

        for (String line: list){

            buf.write( line.getBytes(StandardCharsets.UTF_8) );
            buf.write( HTTP.CRLF );
        }

        return buf.toByteArray();
    }
}
