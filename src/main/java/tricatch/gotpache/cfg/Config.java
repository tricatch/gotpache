package tricatch.gotpache.cfg;

import tricatch.gotpache.cfg.attr.*;

public class Config {

    private Https https;
    private Console console;
    private Ca ca;
    private String version = "0.6.0";
    private String serverName = "Gotpache Console";

    public Https getHttps() {
        return https;
    }

    public void setHttps(Https https) {
        this.https = https;
    }

    public Console getConsole(){
        return this.console;
    }

    public void setConsole(Console console){
        this.console = console;
    }

    public Ca getCa() {
        return ca;
    }

    public void setCa(Ca ca) {
        this.ca = ca;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getServerName() {
        return serverName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }
}
