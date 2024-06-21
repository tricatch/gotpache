package tricatch.gotpache.cert;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.ExtensionsGenerator;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.bouncycastle.util.io.pem.PemWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertTool {

	private static Logger logger = LoggerFactory.getLogger(CertTool.class);

	// DO-NOT-CHANGE
	private static final String BC_PROVIDER = "BC";
	private static final String KEY_ALGORITHM = "RSA";
	private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
	private static final int CA_EXPIRE_DAYS = 365 * 30;
	private static final int SSL_EXPIRE_DAYS = 180;

	private static String ROOT_CA_NAME;
	private static String ROOT_CA_FILE;
	private static String ROOT_CA_PRI_KEY_ALIAS;
	private static String ROOT_CA_PRI_KEY_PASS;

	private static PrivateKey rootPrivateKey = null;
	private static X509Certificate rootCert = null;
	private static X500Name rootCertIssuer = null;

	public static void init(String name, String alias) throws Exception {

		ROOT_CA_FILE = name + "-" + alias;
		ROOT_CA_NAME = "CN=" + name + "-" + alias;
		ROOT_CA_PRI_KEY_ALIAS = alias;
		ROOT_CA_PRI_KEY_PASS = "password";

		rootCertIssuer = new X500Name(ROOT_CA_NAME);

		// if no ca certificate, create new one
		if (!new File("./conf/" + ROOT_CA_FILE + ".cer").exists()) {

			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
			keyPairGenerator.initialize(2048);

			KeyPair rootKeyPair = keyPairGenerator.generateKeyPair();

			rootPrivateKey = rootKeyPair.getPrivate();
			PublicKey rootPublicKey = rootKeyPair.getPublic();

			BigInteger rootSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

			Calendar rootCalendar = Calendar.getInstance();
			rootCalendar.add(Calendar.DATE, -1);
			Date rootStartDate = rootCalendar.getTime();

			rootCalendar.add(Calendar.DATE, CA_EXPIRE_DAYS);
			Date rootEndDate = rootCalendar.getTime();

			ContentSigner rootCertContentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
					.setProvider(BC_PROVIDER).build(rootPrivateKey);
			X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(rootCertIssuer, rootSerialNum,
					rootStartDate, rootEndDate, rootCertIssuer, rootPublicKey);

			JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils();
			rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
			rootCertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
					rootCertExtUtils.createSubjectKeyIdentifier(rootPublicKey));

			KeyUsage ku = new KeyUsage(KeyUsage.keyCertSign | KeyUsage.digitalSignature);
			ExtensionsGenerator extgen = new ExtensionsGenerator();
			extgen.addExtension(Extension.keyUsage, true, ku);
			rootCertBuilder.addExtension(Extension.keyUsage, true, ku);

			X509CertificateHolder rootCertHolder = rootCertBuilder.build(rootCertContentSigner);

			rootCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER).getCertificate(rootCertHolder);

			writeCertToFileBase64Encoded(rootCert, "./conf/" + ROOT_CA_FILE + ".cer");
			exportKeyPairToKeystoreFile(rootPrivateKey, rootCert, ROOT_CA_PRI_KEY_ALIAS,
					"./conf/" + ROOT_CA_FILE + ".pfx", "PKCS12", ROOT_CA_PRI_KEY_PASS);

			logger.info("CREATE new root ca - " + ROOT_CA_FILE + "(.cer/.pfx)");

		} else {

			PemReader priKeyPemReader = null;
			PemReader certPemReader = null;

			try {
				priKeyPemReader = new PemReader(new java.io.FileReader("./conf/" + ROOT_CA_FILE + ".pfx"));
				PemObject priKeyPemObject = priKeyPemReader.readPemObject();

				KeyFactory kf = KeyFactory.getInstance("RSA");
				PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(priKeyPemObject.getContent());

				rootPrivateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);

				certPemReader = new PemReader(new java.io.FileReader("./conf/" + ROOT_CA_FILE + ".cer"));
				PemObject certPemObject = certPemReader.readPemObject();

				rootCert = (X509Certificate) CertificateFactory.getInstance("X.509")
						.generateCertificate(new ByteArrayInputStream(certPemObject.getContent()));

				logger.info("LOAD root ca - " + ROOT_CA_FILE + "(.cer/.pfx)");
			} catch (Exception e) {
				logger.error("errorLoadCert-" + e.getMessage(), e);
			} finally {
				if (priKeyPemReader != null)
					try {
						priKeyPemReader.close();
					} catch (Exception e) {
					}
				if (certPemReader != null)
					try {
						certPemReader.close();
					} catch (Exception e) {
					}
			}

		}

	}

	public static void genCert(String domain, Map<String, X509Certificate> domainCertificate,
			Map<String, PrivateKey> domainPrivateKey) throws Exception {

		X500Name issuedCertSubject = new X500Name("CN=" + domain);
		BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
		keyPairGenerator.initialize(2048);

		KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair();

		PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject,
				issuedCertKeyPair.getPublic());
		JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER);

		ContentSigner csrContentSigner = csrBuilder.build(rootPrivateKey);
		PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

		Calendar sslCalendar = Calendar.getInstance();
		sslCalendar.add(Calendar.DATE, -1);
		Date sslStartDate = sslCalendar.getTime();

		sslCalendar.add(Calendar.DATE, SSL_EXPIRE_DAYS);
		Date sslEndDate = sslCalendar.getTime();

		X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(rootCertIssuer, issuedCertSerialNum,
				sslStartDate, sslEndDate, csr.getSubject(), csr.getSubjectPublicKeyInfo());

		JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils();

		issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

		issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false,
				issuedCertExtUtils.createAuthorityKeyIdentifier(rootCert));
		issuedCertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
				issuedCertExtUtils.createSubjectKeyIdentifier(csr.getSubjectPublicKeyInfo()));

		issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature));

		issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, new DERSequence(new ASN1Encodable[] {
				new GeneralName(GeneralName.dNSName, domain), new GeneralName(GeneralName.iPAddress, "127.0.0.1") }));

		X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
		X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
				.getCertificate(issuedCertHolder);

		issuedCert.verify(rootCert.getPublicKey(), BC_PROVIDER);

		domainCertificate.put(domain, issuedCert);
		domainPrivateKey.put(domain, issuedCertKeyPair.getPrivate());

	}

	private static void exportKeyPairToKeystoreFile(PrivateKey privateKey, java.security.cert.Certificate certificate,
			String alias, String fileName, String storeType, String storePass) throws Exception {

		// KeyStore sslKeyStore = KeyStore.getInstance(storeType, BC_PROVIDER);
		// sslKeyStore.load(null, null);
		// sslKeyStore.setKeyEntry(alias, privateKey,null, new
		// java.security.cert.Certificate[]{certificate});
		// FileOutputStream keyStoreOs = new FileOutputStream(fileName);
		// sslKeyStore.store(keyStoreOs, storePass.toCharArray());

		PemWriter privateKeyWriter = new PemWriter(new java.io.FileWriter(fileName));
		privateKeyWriter.writeObject(new PemObject("PRIVATE KEY", privateKey.getEncoded()));
		privateKeyWriter.close();
	}

	private static void writeCertToFileBase64Encoded(Certificate certificate, String fileName) throws Exception {

		PemWriter privateKeyWriter = new PemWriter(new java.io.FileWriter(fileName));
		privateKeyWriter.writeObject(new PemObject("CERTIFICATE", certificate.getEncoded()));
		privateKeyWriter.close();
	}
}
