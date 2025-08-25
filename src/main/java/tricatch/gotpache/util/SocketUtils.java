package tricatch.gotpache.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class SocketUtils {

    private static Logger logger = LoggerFactory.getLogger(SocketUtils.class);

    public static Socket createHttp(String host, int port, int connectTimeout, int readTimeout) throws IOException {

        if( port<0 ) port = 80;

        if (logger.isDebugEnabled()) logger.debug("createHttp: host={}, port={}", host, port);

        InetSocketAddress endpoint = new InetSocketAddress(host, port);

        Socket socket = new Socket();
        socket.setSoTimeout(readTimeout);
        socket.connect(endpoint, connectTimeout);

        return socket;
    }

    public static Socket createHttps(String domain, String host, int port, int connectTimeout, int readTimeout) throws IOException {

        if( port<0 ) port = 443;

        if (logger.isDebugEnabled()) logger.debug("createHttps: domain={}, host={}, port={}", domain, host, port);

        InetSocketAddress endpoint = new InetSocketAddress(host, port);

        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                }
        };

        SSLContext sc = null;

        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new SecureRandom());
        } catch (Exception e) {
            throw new IOException(e);
        }

        SSLSocketFactory socketFactory = sc.getSocketFactory();

        Socket tcpSocket = new Socket();
        tcpSocket.setSoTimeout(readTimeout);
        tcpSocket.connect(endpoint, connectTimeout);

        Socket socket = socketFactory.createSocket(tcpSocket, domain, endpoint.getPort(), false);

        ((SSLSocket) socket).startHandshake();

        return socket;
    }
}
