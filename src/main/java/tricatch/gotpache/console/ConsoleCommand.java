package tricatch.gotpache.console;

import tricatch.gotpache.cfg.Config;

import java.io.IOException;

public interface ConsoleCommand {

    public ConsoleResponse execute(String uri, Config config) throws IOException;

}
