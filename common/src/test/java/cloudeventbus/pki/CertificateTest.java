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

import static org.testng.Assert.*;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CertificateTest {

	@Test
	public void createSerializeDeserializeHash() throws Exception {
		final KeyPair keyPair = TrustStore.generateKeyPair();

		final Certificate.Type type = Certificate.Type.AUTHORITY;
		final long serialNumber = 1234l;
		final long issuer = 5678;
		final long expirationDate = System.currentTimeMillis();
		final List<String> subscribe = Arrays.asList("foo.bar", "test");
		final List<String> publish = Arrays.asList("this", "that", "theOtherOne");
		final String comment = "This is a comment.";
		final byte[] signature = new byte[Certificate.SIGNATURE_LENGTH];

		final Certificate certificate = new Certificate(type, serialNumber, issuer, expirationDate, keyPair.getPublic(), subscribe, publish, comment, signature);
		final byte[] firstHash = certificate.hash();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		certificate.store(out);

		final Certificate copy = new Certificate(new ByteArrayInputStream(out.toByteArray()));

		assertEquals(copy, certificate);
		assertEquals(copy.hashCode(), certificate.hashCode());
		assertEquals(copy.hash(), firstHash);

		assertEquals(type, copy.getType());
		assertEquals(copy.getSerialNumber(), serialNumber);
		assertEquals(copy.getIssuer(), issuer);
		assertEquals(copy.getExpirationDate(), expirationDate);
		assertEquals(copy.getPublicKey(), keyPair.getPublic());
		assertEquals(copy.getSubscribePermissions(), subscribe);
		assertEquals(copy.getPublishPermissions(), publish);
		assertEquals(copy.getComment(), comment);
		assertEquals(copy.getSignature(), signature);
	}

	@Test(expectedExceptions = CertificateIssuerMismatchException.class)
	public void mismatchedIssuer() throws Exception {
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
		final Certificate secondIssuerCertificate = TrustStore.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
		secondIssuerCertificate.validateSignature(certificate);
	}

	@Test(expectedExceptions = CertificateSecurityException.class)
	public void invalidSignature() {
		final KeyPair issuerKeyPair = TrustStore.generateKeyPair();
		final KeyPair certificateKeyPair = TrustStore.generateKeyPair();

		final Certificate issuerCertificate = TrustStore.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
		final Certificate certificate = new Certificate(
				Certificate.Type.CLIENT,
				1,
				issuerCertificate.getSerialNumber(),
				-1,
				certificateKeyPair.getPublic(),
				Arrays.asList("*"),
				Arrays.asList("*"),
				"This is a bad signature",
				new byte[Certificate.SIGNATURE_LENGTH]);
		issuerCertificate.validateSignature(certificate);
	}

	@Test(expectedExceptions = InvalidCertificateSignatureException.class)
	public void modifiedCertificate() {
		final KeyPair issuerKeyPair = TrustStore.generateKeyPair();
		final KeyPair certificateKeyPair = TrustStore.generateKeyPair();

		final Certificate issuerCertificate = TrustStore.generateSelfSignedCertificate(issuerKeyPair, -1, "Issuer");
		final Certificate validCertificate = TrustStore.generateSignedCertificate(
				issuerCertificate,
				issuerKeyPair.getPrivate(),
				certificateKeyPair.getPublic(),
				Certificate.Type.CLIENT,
				-1,
				Arrays.asList("*"),
				Arrays.asList("*"),
				"Client certificate");
		final Certificate certificate = new Certificate(
				validCertificate.getType(),
				2l,
				validCertificate.getIssuer(),
				validCertificate.getExpirationDate(),
				validCertificate.getPublicKey(),
				validCertificate.getSubscribePermissions(),
				validCertificate.getPublishPermissions(),
				validCertificate.getComment(),
				validCertificate.getSignature());
		issuerCertificate.validateSignature(certificate);
	}
}
