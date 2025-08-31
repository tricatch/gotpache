package tricatch.gotpache.server;

import java.net.URL;
import java.util.List;

public class VirtualPath {

    private String path;
    private URL target;
    private List<String> addHeader;
    private List<String> removeHeader;

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

    public List<String> getAddHeader() {
        return addHeader;
    }

    public void setAddHeader(List<String> addHeader) {
        this.addHeader = addHeader;
    }

    public List<String> getRemoveHeader() {
        return removeHeader;
    }

    public void setRemoveHeader(List<String> removeHeader) {
        this.removeHeader = removeHeader;
    }
}
