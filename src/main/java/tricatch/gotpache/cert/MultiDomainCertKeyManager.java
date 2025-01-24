package tricatch.gotpache.cert;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.X509ExtendedKeyManager;

import io.github.tricatch.gotpache.cert.CertificateKeyPair;
import io.github.tricatch.gotpache.cert.SSLCertificateCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class MultiDomainCertKeyManager extends X509ExtendedKeyManager {

	private static Logger logger = LoggerFactory.getLogger(MultiDomainCertKeyManager.class);
	
    private final Map<String, CertificateKeyPair> certificates = new HashMap<>();
    private final X509Certificate rootCertificate;
    private final PrivateKey rootPrivaeKey;

    public MultiDomainCertKeyManager(X509Certificate rootCertificate, PrivateKey rootPrivaeKey){
        this.rootCertificate = rootCertificate;
        this.rootPrivaeKey = rootPrivaeKey;
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] principals, Socket socket) {

        SSLSocket sslSocket = (SSLSocket)socket;

        //Get host name from SSL handshake
        ExtendedSSLSession session = (ExtendedSSLSession) sslSocket.getHandshakeSession();
        String domain = null;
        for (SNIServerName name : session.getRequestedServerNames()) {
            if (name.getType() == StandardConstants.SNI_HOST_NAME) {
                domain = ((SNIHostName) name).getAsciiName();
                break;
            }
        }

        if( certificates.containsKey(domain) ) return domain;
        
        try {
            SSLCertificateCreator sslCertificateCreator = new SSLCertificateCreator();
            CertificateKeyPair certificateKeyPair = sslCertificateCreator.generateSSLCertificate(domain, rootCertificate, rootPrivaeKey);
            certificates.put(domain, certificateKeyPair);
        	return domain;
        }catch(Exception e) {
        	logger.error( "errorGenCert-" + e.getMessage(), e );
        }
        
        return null;
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
        throw new UnsupportedOperationException("Method getServerAliases() not yet implemented.");
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
        throw new UnsupportedOperationException("Method getClientAliases() not yet implemented.");
    }

    public String chooseClientAlias(String keyTypes[], Principal[] issuers, Socket socket) {
        throw new UnsupportedOperationException("Method chooseClientAlias() not yet implemented.");
    }


    public X509Certificate[] getCertificateChain(String alias) {

    	if( certificates.containsKey(alias) ) {
    		X509Certificate[] x509 = new X509Certificate[1];
    		x509[0] = certificates.get(alias).getCertificate();
    		return x509;
    	}
    	
    	return null;
    }

	@Override
	public PrivateKey getPrivateKey(String alias) {

		if( certificates.containsKey(alias) ) {
    		return certificates.get(alias).getPrivateKey();
    	}
		
		return null;
	}

}
