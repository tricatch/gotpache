package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.*;

import java.io.IOException;
import java.net.SocketTimeoutException;
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
        this.rid = passRequestExecutor.getUid();
    }

    @Override
    public void run() {

        HeaderLines responseHeaders = new HeaderLines(HTTP.INIT_HEADER_LINES);
        int bytesRead;

        try {

            if( logger.isDebugEnabled() ){
                logger.debug( "{}, vtStart", this.passRequestExecutor.getUid() );
            }

            while(true) {

                //read-res-header
                bytesRead = serverIn.readHeaders(responseHeaders, HTTP.MAX_HEADER_LENGTH);

                this.rid = this.passRequestExecutor.getRid();

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

        } catch (SocketTimeoutException e){
            logger.error( this.passRequestExecutor.getUid() + ", " + e.getMessage());
        } catch (IOException e) {
            logger.error( this.passRequestExecutor.getUid() + ", " + e.getMessage(), e);
        } finally {
            if( logger.isDebugEnabled() ){
                logger.debug( "{}, vtEnd", this.passRequestExecutor.getUid() );
            }
            passRequestExecutor.setStop(true);
            LockSupport.unpark(this.passRequestExecutor.getThread());
        }

    }
}
