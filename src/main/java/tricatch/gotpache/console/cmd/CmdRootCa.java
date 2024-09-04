package tricatch.gotpache.console.cmd;

import tricatch.gotpache.cert.CertTool;
import tricatch.gotpache.cfg.Config;
import tricatch.gotpache.console.ConsoleCommand;
import tricatch.gotpache.console.ConsoleResponse;
import tricatch.gotpache.console.ConsoleResponseBuilder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CmdRootCa implements ConsoleCommand {

    @Override
    public ConsoleResponse execute(String uri, Config config) throws IOException {

        String caFilename = CertTool.getRootCaFile() + ".cer";
        String filepath = "./conf/" + caFilename;
        FileInputStream fileInputStream = null;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {

            fileInputStream = new FileInputStream(new File(filepath));

            byte[] buf = new byte[1024*4];
            for(;;){
                int n = fileInputStream.read(buf);
                if( n<0 ) break;
                if( n==0 ) continue;
                bout.write(buf, 0, n);
            }

            return ConsoleResponseBuilder.file(bout.toByteArray(), caFilename);

        } catch (IOException e) {
            throw e;
        } finally {
            if( fileInputStream!=null ) try{ fileInputStream.close(); }catch (Exception e){}
            if( bout!=null ) try{ bout.reset(); }catch (Exception e){}
        }

    }

}
