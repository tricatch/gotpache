package tricatch.gotpache.console.cmd;

import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CmdRootCa implements ConsoleCommand {

    @Override
    public ConsoleResponse execute(String uri, Config config) throws IOException {

        Path filePath = Paths.get("./conf", config.getCa().getCert());

        FileInputStream fileInputStream = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {

            fileInputStream = new FileInputStream(filePath.toFile());

            byte[] buf = new byte[1024*4];
            for(;;){
                int n = fileInputStream.read(buf);
                if( n<0 ) break;
                if( n==0 ) continue;
                bout.write(buf, 0, n);
            }

            return ConsoleResponseBuilder.file(bout.toByteArray(), config.getCa().getCert());

        } finally {
            if( fileInputStream!=null ) try{ fileInputStream.close(); } catch (Exception e){}
            try{ bout.reset(); } catch (Exception e){}
        }

    }

}
