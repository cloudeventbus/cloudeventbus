package cloudeventbus.pki;

import static junit.framework.Assert.*;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class TrustStoreTest {

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

	@Test
	public void validateCertificate() {
		final TrustStore trustStore = new TrustStore();
		trustStore.add(issuerCertificate);
		trustStore.validateCertificate(certificate);
	}

	@Test(expectedExceptions = UntrustedCertificateException.class)
	public void missingIssuerInTrustStore() {
		final TrustStore trustStore = new TrustStore();
		trustStore.validateCertificate(certificate);
	}

	@Test(expectedExceptions = DuplicateCertificateException.class)
	public void duplicateIssuerInTrustStore() {
		final TrustStore trustStore = new TrustStore();
		trustStore.add(issuerCertificate);
		trustStore.add(issuerCertificate);
	}

	@Test
	public void validateCertificateChain() {
		final CertificateChain certificates = new CertificateChain();
		certificates.add(certificate);

		final TrustStore trustStore = new TrustStore();
		trustStore.add(issuerCertificate);

		trustStore.validateCertificateChain(certificates);
	}

	@Test(expectedExceptions = InvalidCertificateException.class)
	public void nonAuthorityCertificateInTrustStore() {
		final TrustStore trustStore = new TrustStore();
		trustStore.add(certificate);
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void validateEmptyCertificateChain() {
		final TrustStore trustStore = new TrustStore();
		trustStore.add(issuerCertificate);
		trustStore.validateCertificateChain(new CertificateChain());
	}

	@Test
	public void size() {
		final TrustStore trustStore = new TrustStore();
		assertEquals(0, trustStore.size());
		trustStore.add(issuerCertificate);
		assertEquals(1, trustStore.size());
	}

	@Test
	public void iterator() {
		final TrustStore trustStore = new TrustStore();
		Iterator<Certificate> iterator = trustStore.iterator();
		assertFalse(iterator.hasNext());
		trustStore.add(issuerCertificate);
		iterator = trustStore.iterator();
		assertTrue(iterator.hasNext());
		assertEquals(issuerCertificate, iterator.next());
		assertFalse(iterator.hasNext());
	}
}
