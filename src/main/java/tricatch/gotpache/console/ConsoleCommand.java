package tricatch.gotpache.console;

import java.io.IOException;
import java.util.Map;

public interface ConsoleCommand {


    ConsoleResponse execute(String uri, Map<String, String> params) throws IOException;

}
