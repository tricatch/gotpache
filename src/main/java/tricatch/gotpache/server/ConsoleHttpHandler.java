package tricatch.gotpache.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;
import tricatch.gotpache.console.SseCommand;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsoleHttpHandler implements HttpHandler {

    private static final Logger logger = LoggerFactory.getLogger(ConsoleHttpHandler.class);

    private final Map<String, ConsoleCommand> commands;

    public ConsoleHttpHandler(Map<String, ConsoleCommand> commands) {
        this.commands = commands;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        try {

            if (logger.isTraceEnabled()) {
                logger.trace("new request - h{}", exchange.hashCode());
            }

            String uri = exchange.getRequestURI().getPath();

            if (logger.isDebugEnabled()) {
                logger.debug("console, req={}", uri);
            }

            ConsoleCommand cmd = commands.get(uri);

            ConsoleResponse res;

            if (cmd != null) {
                if (cmd instanceof SseCommand) {
                    ((SseCommand) cmd).executeSse(exchange);
                    return;
                }

                String method = exchange.getRequestMethod();
                
                Map<String, String> params = new HashMap<>();
                if ("GET".equalsIgnoreCase(method)) {
                    params = parseQuery(exchange.getRequestURI().getRawQuery());
                } else if ("POST".equalsIgnoreCase(method)) {
                    InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                    BufferedReader br = new BufferedReader(isr);
                    StringBuilder buf = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        buf.append(line);
                    }
                    params = parseQuery(buf.toString());
                }
                
                // Add client IP to params
                String clientIp = getClientIp(exchange);
                params.put("__clientIp", clientIp);

                res = cmd.execute(uri, params);

            } else {

                res = ConsoleResponseBuilder._404();
            }

            // Send response headers
            List<String> headers = res.getHeaders();
            
            if (logger.isDebugEnabled()) {
                logger.debug("console, res={}", headers.getFirst());
            }

            // Parse status code from first header line
            String statusLine = headers.get(0);
            int statusCode = Integer.parseInt(statusLine.split(" ")[1]);
            
            // Set response headers (excluding status line)
            for (int i = 1; i < headers.size(); i++) {
                String header = headers.get(i);
                String[] parts = header.split(": ", 2);
                if (parts.length == 2) {
                    exchange.getResponseHeaders().set(parts[0], parts[1]);
                }
            }

            // Send response
            exchange.sendResponseHeaders(statusCode, res.getBody().length);
            
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(res.getBody());
                responseBody.flush();
            }

        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            
            // Send 500 error response
            try {
                ConsoleResponse errorResponse = ConsoleResponseBuilder._404();
                exchange.sendResponseHeaders(500, errorResponse.getBody().length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(errorResponse.getBody());
                }
            } catch (IOException ioException) {
                logger.error("Failed to send error response", ioException);
            }
        }
    }

    private String getClientIp(HttpExchange exchange) {
        // Try to get real client IP from headers first
        String xForwardedFor = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // X-Forwarded-For can contain multiple IPs, take the first one
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = exchange.getRequestHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        // Fall back to remote address
        return exchange.getRemoteAddress().getAddress().getHostAddress();
    }

    private Map<String, String> parseQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> result = new HashMap<>();
        if (query == null || query.isEmpty()) return result;

        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String value = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            result.put(key, value);
        }
        return result;
    }
}
