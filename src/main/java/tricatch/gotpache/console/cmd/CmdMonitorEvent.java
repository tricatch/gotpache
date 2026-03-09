package tricatch.gotpache.console.cmd;

import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.SseCommand;
import tricatch.gotpache.event.HttpEventManager;
import tricatch.gotpache.event.HttpEventMonitorConsumer;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

        exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");

        exchange.sendResponseHeaders(200, 0);

        OutputStream out = exchange.getResponseBody();

        HttpEventMonitorConsumer consumer = new HttpEventMonitorConsumer(clientId, out);
        HttpEventManager.getInstance().addMonitorConsumer(consumer);

        try {
            out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            HttpEventManager.getInstance().removeMonitorConsumer(clientId);
            return;
        }

        try {
            // 5초 간격 keepalive - 프록시/로드밸런서 타임아웃(보통 30~60초) 전에 연결 유지
            for (;;) {
                Thread.sleep(5000);
                out.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            logger.debug("SSE client disconnected: {}", clientId);
        } finally {
            HttpEventManager.getInstance().removeMonitorConsumer(clientId);
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }
}
