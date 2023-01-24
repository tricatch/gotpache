package tricatch.gotpache.pass;

public class PassHost {

    private String virtualHost;
    private boolean ssl = false;
    private String targetHost;
    private int targetPort;
    private String path;

    public String getVirtualHost() {
        return virtualHost;
    }

    public void setVirtualHost(String virtualHost) {
        this.virtualHost = virtualHost;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public String getTargetHost() {
        return targetHost;
    }

    public void setTargetHost(String targetHost) {
        this.targetHost = targetHost;
    }

    public int getTargetPort() {
        return targetPort;
    }

    public void setTargetPort(int targetPort) {
        this.targetPort = targetPort;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "PassHost{" +
                "virtualHost='" + virtualHost + '\'' +
                ", ssl=" + ssl +
                ", targetHost='" + targetHost + '\'' +
                ", targetPort=" + targetPort +
                ", path='" + path + '\'' +
                '}';
    }
}
