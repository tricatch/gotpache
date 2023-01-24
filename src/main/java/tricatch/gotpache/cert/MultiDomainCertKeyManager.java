package tricatch.gotpache.cert;

import javax.net.ssl.ExtendedSSLSession;
import javax.net.ssl.SNIHostName;
import javax.net.ssl.SNIServerName;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.StandardConstants;
import javax.net.ssl.X509ExtendedKeyManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

public class MultiDomainCertKeyManager extends X509ExtendedKeyManager {

	private static Logger logger = LoggerFactory.getLogger(MultiDomainCertKeyManager.class);
	
    private Map<String, X509Certificate> domainCertificate = new HashMap<>();
    private Map<String, PrivateKey> domainPrivateKey = new HashMap<>();

    public MultiDomainCertKeyManager()
            throws IOException, GeneralSecurityException {
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

        if( domainCertificate.containsKey(domain) ) return domain;
        
        try {
        	
        	CertTool.genCert(domain, domainCertificate, domainPrivateKey);
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

    	if( domainCertificate.containsKey(alias) ) {
    		X509Certificate[] x509 = new X509Certificate[1];
    		x509[0] = domainCertificate.get(alias);
    		return x509;
    	}
    	
    	return null;
    }

	@Override
	public PrivateKey getPrivateKey(String alias) {

		if( domainPrivateKey.containsKey(alias) ) {
    		return domainPrivateKey.get(alias);
    	}
		
		return null;
	}

}
