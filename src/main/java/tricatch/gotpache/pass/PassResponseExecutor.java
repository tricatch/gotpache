package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.*;

import java.io.IOException;
import java.util.concurrent.locks.LockSupport;

public class PassResponseExecutor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(PassResponseExecutor.class);

    private PassRequestExecutor passRequestExecutor;

    private HttpStreamWriter clientOut = null;
    private HttpStreamReader serverIn = null;

    private String rid;

    public PassResponseExecutor(PassRequestExecutor passRequestExecutor, HttpStreamReader serverIn, HttpStreamWriter clientOut){
        this.passRequestExecutor = passRequestExecutor;
        this.serverIn = serverIn;
        this.clientOut = clientOut;
    }

    @Override
    public void run() {

        HeaderLines responseHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);
        int bytesRead;

        try {

            while(true) {

                this.rid = this.passRequestExecutor.getRid();

                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Wait - unpark"
                            , rid
                            , HttpStream.Flow.RES
                    );
                }
                LockSupport.park();

                //read-res-header
                bytesRead = serverIn.readHeaders(responseHeaders, HTTP.MAX_HEADER_LENGTH);

                if (bytesRead == -1) {
                    logger.warn("{}, No headers received from server"
                            , rid
                    );
                    return;
                }

                //parse-res-header
                HttpResponse response = responseHeaders.parseHttpResponse();
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Response Headers\n{}"
                            , rid
                            , HttpStream.Flow.RES
                            , responseHeaders
                    );
                    logger.debug("{}, {}, Response: {} {} {} (Body: {}, Connection: {}, ContentLength: {})"
                            , rid
                            , HttpStream.Flow.RES
                            , response.getVersion()
                            , response.getStatusCode()
                            , response.getStatusMessage()
                            , response.getBodyStream()
                            , response.getConnection()
                            , response.getContentLength()
                    );
                }

                //write-res-header
                clientOut.writeHeaders(responseHeaders);

                // Relay response body to client
                HttpStream.Connection connection = RelayBody.relayResponseBody(rid, HttpStream.Flow.RES, response, serverIn, clientOut);
                if (connection == HttpStream.Connection.CLOSE) {
                    passRequestExecutor.setStop(true);
                }

                if( passRequestExecutor.isStop()
                    || "Close".equalsIgnoreCase(response.getConnection())
                ){
                    break;
                }

                LockSupport.unpark(this.passRequestExecutor.getThread());
            }

        } catch (IOException e) {
            String errorRid = rid != null ? rid : "unknown";
            logger.error( errorRid + ", " + e.getMessage(), e);
        } finally {
            passRequestExecutor.setStop(true);
            LockSupport.unpark(this.passRequestExecutor.getThread());
        }

    }
}
