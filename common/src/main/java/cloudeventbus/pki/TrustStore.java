package cloudeventbus.pki;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class TrustStore {

	public static final int KEY_SIZE = 2048;

	public static KeyPair generateKeyPair() {
		try {
			final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(KEY_SIZE);
			return keyPairGenerator.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static Certificate generateSelfSignedCertificate(KeyPair keyPair, long expirationDate, String comment) {
		final SecureRandom random = new SecureRandom();
		final long serialNumber = random.nextLong();
		final Certificate certificate = new Certificate(Certificate.Type.AUTHORITY, serialNumber, serialNumber, expirationDate, keyPair.getPublic(), Arrays.asList("*"), Arrays.asList("*"), comment, null);
		return signCertificate(certificate, keyPair.getPrivate(), certificate);
	}

	public static Certificate generateSignedCertificate(Certificate issuer, PrivateKey issuerPrivateKey, PublicKey newCertificatePublicKey, Certificate.Type type, long expirationDate, List<String> subscribePermissions, List<String> publishPermissions, String comment) {
		final SecureRandom random = new SecureRandom();
		final long serialNumber = random.nextLong();
		final Certificate certificate = new Certificate(type, serialNumber, issuer.getSerialNumber(), expirationDate, newCertificatePublicKey, subscribePermissions, publishPermissions, comment, null);
		return signCertificate(issuer, issuerPrivateKey, certificate);
	}

	public static Certificate signCertificate(Certificate issuer, PrivateKey issuerPrivateKey, Certificate certificate) {
		if (issuer.getSerialNumber() != certificate.getIssuer()) {
			throw new CertificateIssuerMismatchException("The authority certificate serial number doesn't much the certificate issuer.");
		}
		// TODO Make sure that the certificate permissions don't exceed the authorityCertificate permissions.
		final byte[] hash = certificate.hash();

		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, issuerPrivateKey);
			final byte[] signature = cipher.doFinal(hash);
			return new Certificate(certificate.getType(), certificate.getSerialNumber(), certificate.getIssuer(), certificate.getExpirationDate(), certificate.getPublicKey(), certificate.getSubscribePermissions(), certificate.getPublishPermissions(), certificate.getComment(), signature);
		} catch (GeneralSecurityException e) {
			throw new CertificateSecurityException(e);
		}
	}
}
