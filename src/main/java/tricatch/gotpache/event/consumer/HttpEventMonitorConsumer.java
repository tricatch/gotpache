package tricatch.gotpache.event.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.event.HttpEvent;
import tricatch.gotpache.event.HttpEventConsumer;
import tricatch.gotpache.util.JsonUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Monitor consumer for SSE. Receives all HttpEvents and emits JSON to OutputStream.
 * Caches REQ_HEADER, emits on RES_HEADER when full request/response info is available.
 */
public class HttpEventMonitorConsumer implements HttpEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventMonitorConsumer.class);

    private final String clientId;
    private final String channelId;
    private final OutputStream out;
    /** 직렬화: SSE keepalive 스레드와 HttpEvent 워커가 동일 스트림에 동시 쓰기하지 않도록 */
    private final ReentrantLock writeLock;

    public HttpEventMonitorConsumer(String clientId, String channelId, OutputStream out, ReentrantLock writeLock) {
        this.clientId = clientId;
        this.channelId = channelId;
        this.out = out;
        this.writeLock = writeLock;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getChannelId() {
        return channelId;
    }

    @Override
    public void process(HttpEvent event) {
        if (event == null) {
            return;
        }
        try {
            String json = JsonUtil.toJson(event).replace("\n", "\ndata: ");
            String sse = "data: " + json + "\n\n";
            writeLock.lock();
            try {
                out.write(sse.getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                writeLock.unlock();
            }
        } catch (JsonProcessingException e) {
            logger.debug("Failed to serialize event: {}", e.getMessage());
        } catch (IOException e) {
            logger.trace("Monitor consumer write failed (client disconnected): {}", e.getMessage());
        }
    }
}
