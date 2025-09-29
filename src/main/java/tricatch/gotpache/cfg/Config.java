package tricatch.gotpache.cfg;

import tricatch.gotpache.cfg.attr.*;

import java.util.List;

public class Config {

    private Https https;
    private Console console;
    private Ca ca;

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
}
