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

    public HttpEventMonitorConsumer(String clientId, OutputStream out) {
        this.clientId = clientId;
        this.out = out;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public void process(HttpEvent event) {

        if (event == null) {
            return;
        }
        // Drop the event - optionally log for debugging
        if (logger.isDebugEnabled()) {
            StringBuilder logBuilder = new StringBuilder();
            logBuilder.append("ClientId: ").append(event.getClientId()).append("\n");
            logBuilder.append("Rid: ").append(event.getRid()).append("\n");
            logBuilder.append("Type: ").append(event.getType()).append("\n");
            logBuilder.append("Timestamp: ").append(event.getTimestamp()).append("\n");
            if (event.getHttpStream() != null) {
                logBuilder.append("HttpStream: ").append(event.getHttpStream()).append("\n");
            }
            if (event.getHeaders() != null) {
                logBuilder.append("Headers:\n").append(event.getHeaders()).append("\n");
            }
            if (event.getBody() != null) {
                logBuilder.append("Body Size: ").append(event.getBody().length).append(" bytes\n");
            }
            logger.debug(logBuilder.toString());

            try{
                out.write( logBuilder.toString().getBytes(StandardCharsets.UTF_8) );
            } catch (IOException e) {
                logger.error( e.getMessage(), e);
            }
        }

    }


}
