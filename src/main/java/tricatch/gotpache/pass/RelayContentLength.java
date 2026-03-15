package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpStream;
import tricatch.gotpache.http.io.HttpRequest;
import tricatch.gotpache.http.io.HttpResponse;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;
import tricatch.gotpache.event.HttpEvent;
import tricatch.gotpache.event.HttpEventManager;
import tricatch.gotpache.event.HttpEventType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Class for handling content-length based HTTP body relay operations
 */
public class RelayContentLength {

    private static final Logger logger = LoggerFactory.getLogger(RelayContentLength.class);

    public static HttpStream.Connection relay(String clientId, String rid, HttpStream.Flow flow, Integer contentLength, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (contentLength == null || contentLength <= 0) {
            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, No content length or zero content length", rid, flow);
            }
            return HttpStream.Connection.KEEP_ALIVE;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying content-length body: {} bytes", rid, flow, contentLength);
        }

        boolean exceedsLimit = contentLength > HTTP.MONITOR_BODY_LIMIT;
        ByteArrayOutputStream bodyCollector = exceedsLimit ? null : new ByteArrayOutputStream(contentLength);

        byte[] buffer = new byte[HTTP.BODY_BUFFER_SIZE];
        int remainingBytes = contentLength;

        while (remainingBytes > 0) {
            int bytesToRead = Math.min(buffer.length, remainingBytes);
            int bytesRead = in.read(buffer, 0, bytesToRead);

            if (bytesRead == -1) {
                logger.warn("{}, {}, Unexpected end of stream while reading content-length body", rid, flow);
                break;
            }

            out.write(buffer, 0, bytesRead);
            out.flush();

            if (bodyCollector != null) {
                bodyCollector.write(buffer, 0, bytesRead);
            }

            remainingBytes -= bytesRead;

            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, Relayed {} bytes of body, remaining: {}", rid, flow, bytesRead, remainingBytes);
            }
        }

        out.flush();

        byte[] bodyForEvent;
        if (exceedsLimit) {
            String prefix = flow == HttpStream.Flow.REQ ? "Request" : "Response";
            bodyForEvent = (prefix + " body exceeds " + (HTTP.MONITOR_BODY_LIMIT / 1024 / 1024) + "MB and is not supported for display.").getBytes(StandardCharsets.UTF_8);
        } else {
            bodyForEvent = bodyCollector.toByteArray();
        }

        HttpEvent bodyEvent = new HttpEvent(clientId, rid, flow == HttpStream.Flow.REQ ? HttpEventType.REQ_BODY : HttpEventType.RES_BODY);
        bodyEvent.setBody(bodyForEvent);
        bodyEvent.setHttpStream(HttpStream.CONTENT_LENGTH);
        HttpEventManager.getInstance().enqueue(bodyEvent);

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Content-length body relay completed", rid, flow);
        }

        return HttpStream.Connection.KEEP_ALIVE;
    }
}
