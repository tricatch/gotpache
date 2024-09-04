package tricatch.gotpache.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.http.HTTP;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ConsoleExecutor implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(ConsoleExecutor.class);

    private Socket clientSocket = null;
    private OutputStream clientOut = null;
    private InputStream clientIn = null;
    private Map<String, ConsoleCommand> commands;
    private Config config;

    public ConsoleExecutor(Socket clientSocket, Map<String, ConsoleCommand> commands, Config config) {

        this.clientSocket = clientSocket;
        this.commands = commands;
        this.config = config;
    }


    @Override
    public void run() {

        try {

            if (logger.isTraceEnabled()) logger.trace("read from socket - h{}", this.clientSocket.hashCode());

            this.clientOut = this.clientSocket.getOutputStream();
            this.clientIn = this.clientSocket.getInputStream();

            ConsoleRequest consoleRequest = new ConsoleRequest(this.clientIn);
            String uri = consoleRequest.getUri();

            if (logger.isDebugEnabled()) logger.debug("console, req={}", uri);

            ConsoleCommand cmd = commands.get(uri);

            ConsoleResponse res = null;

            if (cmd != null) res = cmd.execute(uri, config);
            else res = ConsoleResponseBuilder._404();

            //header
            List<String> headers = res.getHeaders();

            if (logger.isDebugEnabled()) logger.debug("console, res={}", headers.get(0));

            for (int i = 0; i < headers.size(); i++) {
                clientOut.write(headers.get(i).getBytes(StandardCharsets.UTF_8));
                clientOut.write(HTTP.CRLF);
            }
            clientOut.write(HTTP.CRLF);
            clientOut.flush();

            //body
            clientOut.write(res.getResponse());
            clientOut.flush();

        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        } finally {
            closeAll();
        }


    }

    private void closeAll() {

        if (logger.isTraceEnabled()) logger.trace("close");

        if (clientIn != null) try {
            clientIn.close();
        } catch (Exception e) {
        }
        if (clientOut != null) try {
            clientOut.close();
        } catch (Exception e) {
        }
        if (clientSocket != null) try {
            clientSocket.close();
        } catch (Exception e) {
        }

        clientIn = null;
        clientOut = null;
        clientSocket = null;
    }
}
