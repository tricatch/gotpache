package tricatch.gotpache.cfg;

import tricatch.gotpache.cfg.attr.Http;
import tricatch.gotpache.cfg.attr.Https;
import tricatch.gotpache.cfg.attr.VirtualDomain;

import java.util.List;

public class Config {

    private Http http;
    private Https https;
    private String localDomainIP;
    private List<VirtualDomain> virtual;

    public Http getHttp() {
        return http;
    }

    public void setHttp(Http http) {
        this.http = http;
    }

    public Https getHttps() {
        return https;
    }

    public void setHttps(Https https) {
        this.https = https;
    }

    public String getLocalDomainIP() {
        return localDomainIP;
    }

    public void setLocalDomainIP(String localDomainIP) {
        this.localDomainIP = localDomainIP;
    }

    public List<VirtualDomain> getVirtual() {
        return virtual;
    }

    public void setVirtual(List<VirtualDomain> virtual) {
        this.virtual = virtual;
    }
}
