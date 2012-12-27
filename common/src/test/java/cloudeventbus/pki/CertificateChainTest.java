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

import static junit.framework.Assert.*;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author Mike Heath <elcapo@gmail.com>
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
