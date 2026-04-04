package tricatch.gotpache.exception;

import java.io.IOException;
import java.net.URL;

/**
 * Thrown when the proxy cannot connect to the configured upstream (origin) server.
 */
public class BadGatewayException extends Exception {

    private final String rid;
    private final String requestHost;
    private final String targetUrl;
    /** Matched virtual path pattern (Ant-style), e.g. {@code /api/**} */
    private final String routePath;

    public BadGatewayException(String rid, String requestHost, URL target, String routePath, IOException cause) {
        super(cause != null ? cause.getMessage() : "Bad Gateway", cause);
        this.rid = rid;
        this.requestHost = requestHost;
        this.targetUrl = target.toExternalForm();
        this.routePath = routePath != null ? routePath : "";
    }

    public String getRid() {
        return rid;
    }

    public String getRequestHost() {
        return requestHost;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public String getRoutePath() {
        return routePath;
    }
}
