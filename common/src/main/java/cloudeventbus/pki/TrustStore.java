package cloudeventbus.pki;

import javax.crypto.Cipher;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class TrustStore extends AbstractSet<Certificate> {

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
		final Certificate certificate = new Certificate(
				Certificate.Type.AUTHORITY,
				serialNumber,
				serialNumber,
				expirationDate,
				keyPair.getPublic(),
				Arrays.asList("*"),
				Arrays.asList("*"),
				comment,
				null);
		return signCertificate(certificate, keyPair.getPrivate(), certificate);
	}

	public static Certificate generateSignedCertificate(Certificate issuer, PrivateKey issuerPrivateKey, PublicKey newCertificatePublicKey, Certificate.Type type, long expirationDate, List<String> subscribePermissions, List<String> publishPermissions, String comment) {
		final SecureRandom random = new SecureRandom();
		final long serialNumber = random.nextLong();
		final Certificate certificate = new Certificate(
				type,
				serialNumber,
				issuer.getSerialNumber(),
				expirationDate,
				newCertificatePublicKey,
				subscribePermissions,
				publishPermissions,
				comment,
				null);
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
			return new Certificate(
					certificate.getType(),
					certificate.getSerialNumber(),
					certificate.getIssuer(),
					certificate.getExpirationDate(),
					certificate.getPublicKey(),
					certificate.getSubscribePermissions(),
					certificate.getPublishPermissions(),
					certificate.getComment(),
					signature);
		} catch (GeneralSecurityException e) {
			throw new CertificateSecurityException(e);
		}
	}

	private final Map<Long, Certificate> certificates = Collections.synchronizedMap(new HashMap<Long, Certificate>());

	@Override
	public Iterator<Certificate> iterator() {
		return certificates.values().iterator();
	}

	@Override
	public int size() {
		return certificates.size();
	}

	/**
	 * Returns the certificate from the trust store with the specified serial number.
	 *
	 * @param serialNumber the serial number of the certificate
	 * @return the certificate from the trust store with the specified serial number, {@code null} if the trust store
	 *         does not contain a certificate with the specified serial number.
	 */
	public Certificate get(long serialNumber) {
		return certificates.get(serialNumber);
	}

	/**
	 * Adds a certificate to the trust store.
	 *
	 * @param certificate the certificate to add to the trust store.
	 */
	public boolean add(Certificate certificate) {
		if (certificate.getType() != Certificate.Type.AUTHORITY) {
			throw  new InvalidCertificateException("Only certificates with type authority may be used in a trust store.");
		}
		final long serialNumber = certificate.getSerialNumber();
		if (get(serialNumber) != null) {
			throw new DuplicateCertificateException("The trust store already contains a certificate with serial number" + serialNumber);
		}
		certificates.put(serialNumber, certificate);
		return true;
	}

	public void validateCertificate(Certificate certificate) {
		final long issuerSerialNumber = certificate.getIssuer();
		final Certificate issuerCertificate = get(issuerSerialNumber);
		if (issuerCertificate == null) {
			throw new UntrustedCertificateException("This trust store does not contain an authority certificate with the serial number " + issuerCertificate);
		}
		issuerCertificate.validateSignature(certificate);
	}

	public void validateCertificateChain(CertificateChain certificateChain) {
		if (certificateChain.size() == 0) {
			throw new IllegalArgumentException("Cannot validate an empty certificate chain.");
		}
		validateCertificate(certificateChain.getFirst());
	}
}
