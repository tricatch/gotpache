package tricatch.gotpache.console.cmd;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.SseCommand;
import tricatch.gotpache.event.HttpEventManager;
import tricatch.gotpache.event.consumer.HttpEventMonitorConsumer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * SSE endpoint for /monitor/event.
 * Streams HTTP events to client via Server-Sent Events.
 */
public class CmdMonitorEvent implements SseCommand {

    private static final Logger logger = LoggerFactory.getLogger(CmdMonitorEvent.class);

    @Override
    public ConsoleResponse execute(String uri, Map<String, String> params) throws IOException {
        throw new UnsupportedOperationException("Use executeSse() for SSE streaming");
    }

    @Override
    public void executeSse(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, 0);
            exchange.close();
            return;
        }

        String clientId = exchange.getRemoteAddress().getAddress().getHostAddress();
        String channelId = exchange.getRemoteAddress().getAddress().getHostAddress() + "/" + exchange.getRemoteAddress().getPort();

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");

        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();
        ReentrantLock writeLock = new ReentrantLock();

        if( logger.isDebugEnabled()) {
            logger.debug("HEMC[{}] - CONNECT", channelId);
        }

        try {
            writeLock.lock();
            try {
                out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            } finally {
                writeLock.unlock();
            }
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        HttpEventMonitorConsumer consumer = new HttpEventMonitorConsumer(clientId, channelId, out, writeLock);
        HttpEventManager.getInstance().addEventConsumer(consumer);

        try {
            // 5초 간격 keepalive - 프록시/로드밸런서 타임아웃(보통 30~60초) 전에 연결 유지
            for (;;) {
                Thread.sleep(5000);
                if (writeLock.tryLock()) {
                    try {
                        out.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    } finally {
                        writeLock.unlock();
                    }
                }
            }
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
            logger.debug("SSE client disconnected: {}", clientId);
        } finally {
            HttpEventManager.getInstance().removeEventConsumer(consumer);
            try {
                writeLock.lock();
                try {
                    out.close();
                } finally {
                    writeLock.unlock();
                }
            } catch (IOException ignored) {
            }
        }
    }
}
