package tricatch.gotpache.console;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;

/**
 * Console command that streams response via Server-Sent Events.
 * Handles HttpExchange directly instead of returning ConsoleResponse.
 */
public interface SseCommand extends ConsoleCommand {

    void executeSse(HttpExchange exchange) throws IOException;
}
