package cloudeventbus.pki;

import static junit.framework.Assert.*;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateChainTest {

	@Test
	public void validIssuerAndCertificate() {
		final KeyPair issuerKeyPair = TrustStore.generateKeyPair();
		final KeyPair certificateKeyPair = TrustStore.generateKeyPair();

		final Certificate issuerCertificate = TrustStore.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
		final Certificate certificate = TrustStore.generateSignedCertificate(
				issuerCertificate,
				issuerKeyPair.getPrivate(),
				certificateKeyPair.getPublic(),
				Certificate.Type.CLIENT,
				-1,
				Arrays.asList("*"),
				Arrays.asList("*"),
				"Client certificate");

		final CertificateChain certificates = new CertificateChain();
		certificates.add(issuerCertificate);
		certificates.add(certificate);

		assertEquals(2, certificates.size());
		final Iterator<Certificate> iterator = certificates.iterator();
		assertEquals(issuerCertificate, iterator.next());
		assertEquals(certificate, iterator.next());
		assertFalse(iterator.hasNext());
	}

	@Test(expectedExceptions = DuplicateCertificateException.class)
	public void issuerInChainTwice() {
		final KeyPair issuerKeyPair = TrustStore.generateKeyPair();
		final Certificate issuerCertificate = TrustStore.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
		final CertificateChain certificates = new CertificateChain();
		certificates.add(issuerCertificate);
		certificates.add(issuerCertificate);
	}
}
