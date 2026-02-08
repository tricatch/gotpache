package tricatch.gotpache.event;

import tricatch.gotpache.http.io.HeaderLines;

import java.util.ArrayList;
import java.util.List;

/**
 * Container for collecting REQ/RES data by rid
 */
public class RequestResponsePair {
    
    private String clientId;
    private String rid;
    private HeaderLines reqHeaders;
    private List<byte[]> reqBodyChunks;
    private HeaderLines resHeaders;
    private List<byte[]> resBodyChunks;
    private boolean reqHeaderComplete;
    private boolean reqBodyComplete;
    private boolean resHeaderComplete;
    private boolean resBodyComplete;
    private long reqStartTime;
    private long resEndTime;
    
    public RequestResponsePair(String clientId, String rid) {
        this.clientId = clientId;
        this.rid = rid;
        this.reqBodyChunks = new ArrayList<>();
        this.resBodyChunks = new ArrayList<>();
        this.reqHeaderComplete = false;
        this.reqBodyComplete = false;
        this.resHeaderComplete = false;
        this.resBodyComplete = false;
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }
    
    public String getRid() {
        return rid;
    }
    
    public void setRid(String rid) {
        this.rid = rid;
    }
    
    public HeaderLines getReqHeaders() {
        return reqHeaders;
    }
    
    public void setReqHeaders(HeaderLines reqHeaders) {
        this.reqHeaders = reqHeaders;
        this.reqHeaderComplete = true;
        if (this.reqStartTime == 0) {
            this.reqStartTime = System.currentTimeMillis();
        }
    }
    
    public List<byte[]> getReqBodyChunks() {
        return reqBodyChunks;
    }
    
    public void addReqBodyChunk(byte[] chunk) {
        if (chunk != null && chunk.length > 0) {
            this.reqBodyChunks.add(chunk);
        }
    }
    
    public void setReqBodyComplete(boolean complete) {
        this.reqBodyComplete = complete;
    }
    
    public HeaderLines getResHeaders() {
        return resHeaders;
    }
    
    public void setResHeaders(HeaderLines resHeaders) {
        this.resHeaders = resHeaders;
        this.resHeaderComplete = true;
    }
    
    public List<byte[]> getResBodyChunks() {
        return resBodyChunks;
    }
    
    public void addResBodyChunk(byte[] chunk) {
        if (chunk != null && chunk.length > 0) {
            this.resBodyChunks.add(chunk);
        }
    }
    
    public void setResBodyComplete(boolean complete) {
        this.resBodyComplete = complete;
        if (complete) {
            this.resEndTime = System.currentTimeMillis();
        }
    }
    
    public boolean isReqHeaderComplete() {
        return reqHeaderComplete;
    }
    
    public boolean isReqBodyComplete() {
        return reqBodyComplete;
    }
    
    public boolean isResHeaderComplete() {
        return resHeaderComplete;
    }
    
    public boolean isResBodyComplete() {
        return resBodyComplete;
    }
    
    /**
     * Check if both REQ and RES are complete
     */
    public boolean isComplete() {
        return reqHeaderComplete && reqBodyComplete && resHeaderComplete && resBodyComplete;
    }
    
    public long getReqStartTime() {
        return reqStartTime;
    }
    
    public void setReqStartTime(long reqStartTime) {
        this.reqStartTime = reqStartTime;
    }
    
    public long getResEndTime() {
        return resEndTime;
    }
    
    public void setResEndTime(long resEndTime) {
        this.resEndTime = resEndTime;
    }
    
    /**
     * Get total request body size
     */
    public int getReqBodySize() {
        return reqBodyChunks.stream().mapToInt(b -> b.length).sum();
    }
    
    /**
     * Get total response body size
     */
    public int getResBodySize() {
        return resBodyChunks.stream().mapToInt(b -> b.length).sum();
    }
}
