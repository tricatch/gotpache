package tricatch.gotpache.event.consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tricatch.gotpache.event.HttpEvent;
import tricatch.gotpache.event.HttpEventConsumer;

/**
 * Drop consumer for HttpEvents when no IP subscriber is registered.
 * Processes events synchronously (called directly from WorkerPool).
 */
public class HttpEventDropConsumer implements HttpEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(HttpEventDropConsumer.class);

    @Override
    public String getClientId() {
        return HttpEventDropConsumer.class.getSimpleName();
    }

    @Override
    public String getChannelId() {
        return HttpEventDropConsumer.class.getSimpleName();
    }

    /**
     * Process and drop the event (no subscriber registered for this IP)
     */
    public void process(HttpEvent event) {
        if (event == null) {
            return;
        }
        // Drop the event - optionally log for debugging
        if (logger.isDebugEnabled()) {
            logger.debug( "DROPPED HttpEvent (No Subscriber) - ClientId: {}, Rid: {}, type: {}, Timestamp: {}, HttpStream={}"
                    , event.getClientId()
                    , event.getRid()
                    , event.getType()
                    , event.getTimestamp()
                    , event.getHttpStream()
                    );
        }
    }
}
