package tricatch.gotpache.cfg;

import tricatch.gotpache.cfg.attr.VirtualDomain;

import java.util.List;

public class VirtualHost {

    private List<VirtualDomain> virtual;

    public List<VirtualDomain> getVirtual() {
        return virtual;
    }

    public void setVirtual(List<VirtualDomain> virtual) {
        this.virtual = virtual;
    }
}
