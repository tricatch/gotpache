package tricatch.gotpache.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.io.HeaderLines;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Monitor consumer for SSE. Receives all HttpEvents and emits JSON to OutputStream.
 * Caches REQ_HEADER, emits on RES_HEADER when full request/response info is available.
 */
public class HttpEventMonitorConsumer implements HttpEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventMonitorConsumer.class);

    private final String clientId;
    private final OutputStream out;
    private final Runnable onClose;

    private final Map<String, ReqInfo> reqCache = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    static {
        TIME_FORMAT.setTimeZone(TimeZone.getDefault());
    }

    public HttpEventMonitorConsumer(String clientId, OutputStream out) {
        this(clientId, out, null);
    }

    public HttpEventMonitorConsumer(String clientId, OutputStream out, Runnable onClose) {
        this.clientId = clientId;
        this.out = out;
        this.onClose = onClose;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void process(HttpEvent event) {
        if (event == null || closed) {
            return;
        }

        try {
            switch (event.getType()) {
                case REQ_HEADER:
                    cacheReqHeader(event);
                    break;
                case RES_HEADER:
                    emitEvent(event);
                    break;
                default:
                    break;
            }
        } catch (IOException e) {
            if (!closed) {
                closed = true;
                logger.debug("Monitor consumer {} closed: {}", clientId, e.getMessage());
                if (onClose != null) {
                    onClose.run();
                }
            }
        } catch (Exception e) {
            logger.debug("Monitor consumer process error: {}", e.getMessage());
        }
    }

    private void cacheReqHeader(HttpEvent event) {
        HeaderLines headers = event.getHeaders();
        if (headers == null || headers.isEmpty()) return;

        try {
            HttpRequest req = headers.parseHttpRequest();
            String path = req.getPath();
            String host = req.getHost();
            String name = (host != null && !host.isEmpty()) ? req.getMethod() + " " + path + " (" + host + ")" : req.getMethod() + " " + path;
            reqCache.put(event.getRid(), new ReqInfo(req.getMethod(), name, event.getClientId()));
        } catch (Exception e) {
            logger.trace("Failed to parse REQ_HEADER for rid={}: {}", event.getRid(), e.getMessage());
        }
    }

    private void emitEvent(HttpEvent event) throws IOException {
        HeaderLines headers = event.getHeaders();
        if (headers == null || headers.isEmpty()) return;

        HttpResponse res;
        try {
            res = headers.parseHttpResponse();
        } catch (Exception e) {
            logger.trace("Failed to parse RES_HEADER for rid={}: {}", event.getRid(), e.getMessage());
            return;
        }

        ReqInfo reqInfo = reqCache.remove(event.getRid());
        String method = reqInfo != null ? reqInfo.method : "?";
        String name = reqInfo != null ? reqInfo.name : "/";
        String initiator = reqInfo != null ? reqInfo.clientId : "-";

        String status = String.valueOf(res.getStatusCode());
        Integer contentLength = res.getContentLength();
        String size = contentLength != null ? formatSize(contentLength) : "-";
        String time = TIME_FORMAT.format(new Date(event.getTimestamp()));

        String json = buildJson(name, status, method, "fetch", initiator, size, time);
        writeSse(json);
    }

    private String buildJson(String name, String status, String method, String type, String initiator, String size, String time) {
        return "{\"name\":\"" + escapeJson(name) + "\",\"status\":\"" + escapeJson(status) +
                "\",\"method\":\"" + escapeJson(method) + "\",\"type\":\"" + escapeJson(type) +
                "\",\"initiator\":\"" + escapeJson(initiator) + "\",\"size\":\"" + escapeJson(size) +
                "\",\"time\":\"" + escapeJson(time) + "\"}";
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    private static String formatSize(int bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    private void writeSse(String data) throws IOException {
        out.write(("data: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    public boolean isClosed() {
        return closed;
    }

    private static class ReqInfo {
        final String method;
        final String name;
        final String clientId;

        ReqInfo(String method, String name, String clientId) {
            this.method = method;
            this.name = name;
            this.clientId = clientId;
        }
    }
}
