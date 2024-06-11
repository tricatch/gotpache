package tricatch.gotpache.cfg.attr;

import java.util.List;

public class VirtualLocation {

    private String host;
    private List<String> path;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public List<String> getPath() {
        return path;
    }

    public void setPath(List<String> path) {
        this.path = path;
    }
}
