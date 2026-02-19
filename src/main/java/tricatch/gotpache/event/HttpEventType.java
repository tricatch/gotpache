package tricatch.gotpache.event;

/**
 * HTTP event type enumeration
 */
public enum HttpEventType {
    REQ_HEADER,    // REQ header read complete
    REQ_BODY,      // REQ body read complete
    RES_HEADER,    // RES header read complete
    RES_BODY       // RES body read complete
}
