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
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CertificateUtils {

	public static final int KEY_SIZE = 2048;

	public static final int DEFAULT_CHALLENGE_LENGTH = 64;

	private static final ThreadLocal<SecureRandom> secureRandom = new ThreadLocal<SecureRandom>() {
		@Override
		protected SecureRandom initialValue() {
			return new SecureRandom();
		}
	};

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
		final long serialNumber = secureRandom.get().nextLong();
		final Certificate certificate = new Certificate(
				Certificate.Type.AUTHORITY,
				serialNumber,
				serialNumber,
				expirationDate,
				keyPair.getPublic(),
				Arrays.asList(Subject.ALL),
				Arrays.asList(Subject.ALL),
				comment,
				null);
		return signCertificate(certificate, keyPair.getPrivate(), certificate);
	}

	public static Certificate generateSignedCertificate(Certificate issuer, PrivateKey issuerPrivateKey, PublicKey newCertificatePublicKey, Certificate.Type type, long expirationDate, List<Subject> subscribePermissions, List<Subject> publishPermissions, String comment) {
		final long serialNumber = secureRandom.get().nextLong();
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

	public static byte[] generateChallenge() {
		final byte[] challenge = new byte[DEFAULT_CHALLENGE_LENGTH];
		secureRandom.get().nextBytes(challenge);
		return challenge;
	}

	public static byte[] signChallenge(PrivateKey key, byte[] challenge, byte[] salt) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			cipher.update(challenge);
			cipher.update(salt);
			return cipher.doFinal();
		} catch (GeneralSecurityException e) {
			throw new CertificateSecurityException(e);
		}
	}

	public static void validateSignature(PublicKey key, byte[] challenge, byte[] salt, byte[] signature) {
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, key);
			final byte[] decryptedSignature = cipher.doFinal(signature);
			if (decryptedSignature.length != challenge.length + salt.length) {
				throw new InvalidSignatureException("Signature doesn't match challenge");
			}
			for (int i = 0; i < challenge.length; i++) {
				if (decryptedSignature[i] != challenge[i]) {
					throw new InvalidSignatureException("Signature doesn't match challenge");
				}
			}
			for (int i = 0; i < salt.length; i++) {
				if (decryptedSignature[challenge.length + i] != salt[i]) {
					throw new InvalidSignatureException("Signature doesn't match challenge");
				}
			}
		} catch (GeneralSecurityException e) {
			throw new CertificateSecurityException(e);
		}
	}

	private CertificateUtils() {
		// Don't instantiate me.
	}

}
