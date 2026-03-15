package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.*;
import tricatch.gotpache.event.HttpEvent;
import tricatch.gotpache.event.HttpEventManager;
import tricatch.gotpache.event.HttpEventType;
import tricatch.gotpache.server.VThreadExecutor;

import java.io.IOException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.locks.LockSupport;

public class PassResponseExecutor implements Stopable {

    private static final Logger logger = LoggerFactory.getLogger(PassResponseExecutor.class);

    private PassRequestExecutor passRequestExecutor;
    private Thread thisThread = null;

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

            this.thisThread = Thread.currentThread();

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

                // Enqueue RES header HttpEvent
                String clientId = this.passRequestExecutor.getClientId();
                HttpEvent resHeaderEvent = new HttpEvent(clientId, this.rid, HttpEventType.RES_HEADER);
                resHeaderEvent.setHeaders(responseHeaders);
                HttpEventManager.getInstance().enqueue(resHeaderEvent);

                //write-res-header
                clientOut.writeHeaders(responseHeaders);

                // Relay response body to client
                HttpStream.Connection connection = RelayBody.relayResponseBody(clientId, rid, HttpStream.Flow.RES, response, serverIn, clientOut);
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

        } catch (SocketTimeoutException e) {
            logger.error(this.passRequestExecutor.getUid() + ", " + e.getMessage());
        } catch (SocketException e){
            if( "Connection reset".equals(e.getMessage())
                || "Socket closed".equals(e.getMessage())
            ) {
                logger.error(this.passRequestExecutor.getUid() + ", " + e.getMessage());
            } else {
                logger.error( this.passRequestExecutor.getUid() + ", " + e.getMessage(), e);
            }
        } catch (IOException e) {
            logger.error( this.passRequestExecutor.getUid() + ", " + e.getMessage(), e);
        } finally {

            VThreadExecutor.removeVirtualThread(Thread.currentThread());

            if( logger.isDebugEnabled() ){
                logger.debug( "{}, vtEnd", this.passRequestExecutor.getUid() );
            }
            if (passRequestExecutor.getChildThread() == this.thisThread) {
                passRequestExecutor.setStop(true);
                LockSupport.unpark(this.passRequestExecutor.getThread());
            }
        }

    }

    @Override
    public void stop() {

    }

    @Override
    public String getName() {
        if( this.thisThread==null ) return null;
        return thisThread.getName();
    }
}
