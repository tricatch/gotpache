package tricatch.gotpache.pass;

import java.net.URL;

public class VirtualPath {

    private String path;
    private URL target;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public URL getTarget() {
        return target;
    }

    public void setTarget(URL target) {
        this.target = target;
    }
}
