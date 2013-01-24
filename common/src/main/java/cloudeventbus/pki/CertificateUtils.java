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
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import javax.crypto.Cipher;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
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
		return generateSelfSignedCertificate(keyPair, expirationDate, Arrays.asList(Subject.ALL), Arrays.asList(Subject.ALL), comment);
	}

	public static Certificate generateSelfSignedCertificate(KeyPair keyPair, long expirationDate, List<Subject> subscribePermissions, List<Subject> publishPermissions, String comment) {
		final long serialNumber = secureRandom.get().nextLong();
		final Certificate certificate = new Certificate(
				Certificate.Type.AUTHORITY,
				serialNumber,
				serialNumber,
				expirationDate,
				keyPair.getPublic(),
				subscribePermissions,
				publishPermissions,
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
		validatePermissions(issuer, certificate);
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

	public static void validatePermissions(Certificate issuer, Certificate certificate) {
		validateSubSubjects(issuer.getPublishPermissions(), certificate.getPublishPermissions(), "publish");
		validateSubSubjects(issuer.getSubscribePermissions(), certificate.getSubscribePermissions(), "subscribe");
	}

	private static void validateSubSubjects(List<Subject> issuerPermissions, List<Subject> permissions, String type) {
		outer: for (Subject permission : permissions) {
			for (Subject parentPermission : issuerPermissions) {
				if (parentPermission.isSub(permission)) {
					continue outer;
				}
			}
			throw new CertificatePermissionError ("Permission " + permission + " is not a sub " + type + " permission to any permissions in parent certificate.");
		}
	}

	public static PrivateKey loadPrivateKey(String fileName) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		final Path path = Paths.get(fileName);
		final byte[] encodedPrivateKey = Files.readAllBytes(path);
		final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		final PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(encodedPrivateKey);
		return keyFactory.generatePrivate(privateKeySpec);
	}

	public static void savePrivateKey(PrivateKey privateKey, String fileName) throws IOException {
		try (
				final OutputStream outputStream = Files.newOutputStream(Paths.get(fileName), StandardOpenOption.CREATE_NEW)
		) {
			outputStream.write(privateKey.getEncoded());
		}
	}

	public static TrustStore loadTrustStore(String fileName) throws IOException {
		final TrustStore trustStore = new TrustStore();
		final Path path = Paths.get(fileName);
		if (Files.notExists(path)) {
			return trustStore;
		}
		loadCertificates(path, trustStore);
		return trustStore;
	}

	private static void loadCertificates(Path path, Collection<Certificate> certificates) throws IOException {
		try (
				final InputStream fileIn = Files.newInputStream(path);
				final InputStream in = new Base64InputStream(fileIn)
		) {
			CertificateStoreLoader.load(in, certificates);
		}
	}

	/**
	 * Loads a collection of certificates as a {@link CertificateChain} from the specified file.
	 *
	 * @param fileName the file from which the certificates will be loaded
	 * @return a certificate chain holding the certificates in the specified file.
	 * @throws IOException if an I/O error occurs
	 */
	public static CertificateChain loadCertificateChain(String fileName) throws IOException {
		final CertificateChain certificateChain = new CertificateChain();
		final Path path = Paths.get(fileName);
		loadCertificates(path, certificateChain);
		return certificateChain;
	}

	/**
	 * Saves a collection of certificates to the specified file.
	 *
	 * @param fileName the file to which the certificates will be saved
	 * @param certificates the certificate to be saved
	 * @throws IOException if an I/O error occurs
	 */
	public static void saveCertificates(String fileName, Collection<Certificate> certificates) throws IOException {
		final Path path = Paths.get(fileName);
		final Path directory = path.getParent();
		if (directory != null && !Files.exists(directory)) {
			Files.createDirectories(directory);
		}
		try (
				final OutputStream fileOut = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		        final OutputStream out = new Base64OutputStream(fileOut)
		) {
			CertificateStoreLoader.store(out, certificates);
		}
	}
}
