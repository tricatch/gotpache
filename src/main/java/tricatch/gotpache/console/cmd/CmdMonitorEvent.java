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

        exchange.sendResponseHeaders(200, -1);

        OutputStream out = exchange.getResponseBody();

        HttpEventMonitorConsumer consumer = new HttpEventMonitorConsumer(clientId, out);
        HttpEventManager.getInstance().addMonitorConsumer(consumer);

        try {
            out.write(": connected\n\n".getBytes(StandardCharsets.UTF_8));
            out.flush();
        } catch (IOException e) {
            return;
        }

        try {

            for (;;) {
                Thread.sleep(10000);
                out.write(": keepalive\n\n".getBytes(StandardCharsets.UTF_8));
                out.flush();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            try {
                out.close();
            } catch (IOException ignored) {
            }
        }
    }
}
