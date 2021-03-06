/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package cloudeventbus.pki;

import cloudeventbus.Subject;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Iterator;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class TrustStoreTest {

	final KeyPair issuerKeyPair = CertificateUtils.generateKeyPair();
	final KeyPair certificateKeyPair = CertificateUtils.generateKeyPair();

	final Certificate issuerCertificate = CertificateUtils.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
	final Certificate certificate = CertificateUtils.generateSignedCertificate(
			issuerCertificate,
			issuerKeyPair.getPrivate(),
			certificateKeyPair.getPublic(),
			Certificate.Type.CLIENT,
			-1,
			Subject.list("*"),
			Subject.list("*"),
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
		assertEquals(trustStore.size(), 0);
		trustStore.add(issuerCertificate);
		assertEquals(trustStore.size(), 1);
	}

	@Test
	public void iterator() {
		final TrustStore trustStore = new TrustStore();
		Iterator<Certificate> iterator = trustStore.iterator();
		assertFalse(iterator.hasNext());
		trustStore.add(issuerCertificate);
		iterator = trustStore.iterator();
		assertTrue(iterator.hasNext());
		assertEquals(iterator.next(), issuerCertificate);
		assertFalse(iterator.hasNext());
	}
}
