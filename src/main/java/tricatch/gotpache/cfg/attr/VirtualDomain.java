package tricatch.gotpache.cfg.attr;

import java.util.List;

public class VirtualDomain {

    private String domain;

    private List<VirtualLocation> location;

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public List<VirtualLocation> getLocation() {
        return location;
    }

    public void setLocation(List<VirtualLocation> location) {
        this.location = location;
    }
}
