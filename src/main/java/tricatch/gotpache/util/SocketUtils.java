package tricatch.gotpache.util;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SocketUtils {

    public static  Socket create(String host, int port, int connectTimeout, int readTimeout, boolean ssl) throws IOException {

        InetSocketAddress endpoint = new InetSocketAddress( host, port);

        if( ssl ){

            SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

            Socket tcpSocket = new Socket();
            tcpSocket.setSoTimeout(readTimeout);
            tcpSocket.connect(endpoint, connectTimeout);

            Socket socket = socketFactory.createSocket(tcpSocket, endpoint.getHostName(), endpoint.getPort(), false);

            ((SSLSocket)socket).startHandshake();

            return socket;

        } else {

            Socket socket = new Socket();
            socket.setSoTimeout(readTimeout);
            socket.connect(endpoint, connectTimeout);

            return socket;
        }
    }

    public static  Socket create(String domain, String host, int port, int connectTimeout, int readTimeout) throws IOException {

        InetSocketAddress endpoint = new InetSocketAddress( host, port);

        SSLSocketFactory socketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();

        Socket tcpSocket = new Socket();
        tcpSocket.setSoTimeout(readTimeout);
        tcpSocket.connect(endpoint, connectTimeout);

        Socket socket = socketFactory.createSocket(tcpSocket, domain, endpoint.getPort(), false);

        ((SSLSocket)socket).startHandshake();

        return socket;
    }
}
