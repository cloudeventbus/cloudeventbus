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

import cloudeventbus.CloudEventBusException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Certificate {

	public enum Type {
		AUTHORITY,
		CLIENT,
		SERVER
	}

	static final int KEY_LENGTH = 294;
	static final int MAX_STRING_LENGTH = 16384;
	static final int SIGNATURE_LENGTH = 256;

	private final Type type;
	private final long serialNumber;
	private final long issuer;
	private final long expirationDate;
	private final PublicKey publicKey;
	private final List<String> subscribePermissions;
	private final List<String> publishPermissions;
	private final String comment;
	private final byte[] signature;

	private volatile byte[] hash;

	public Certificate(Type type, long serialNumber, long issuer, long expirationDate, PublicKey publicKey, List<String> subscribePermissions, List<String> publishPermissions, String comment, byte[] signature) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}
		this.type = type;
		this.serialNumber = serialNumber;
		this.issuer = issuer;
		this.expirationDate = expirationDate;
		if (publicKey == null) {
			throw new IllegalArgumentException("public key cannot be null");
		}
		this.publicKey = publicKey;
		this.subscribePermissions = copyListUnmodifably(subscribePermissions);
		this.publishPermissions = copyListUnmodifably(publishPermissions);
		this.comment = comment;
		if (signature != null && signature.length != SIGNATURE_LENGTH) {
			throw new InvalidCertificateException("Invalid signature, signature must be " + SIGNATURE_LENGTH + " bytes long.");
		}
		this.signature = signature;
	}

	public Certificate(InputStream in) throws IOException {
		final DataInputStream data = new DataInputStream(in);
		try {
			this.type = Type.values()[data.read()];
			serialNumber = data.readLong();
			issuer = data.readLong();
			expirationDate = data.readLong();
			// Read and decode public key
			final byte[] key = new byte[KEY_LENGTH];
			data.readFully(key);
			final KeyFactory keyFactory;
			keyFactory = KeyFactory.getInstance("RSA");
			final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(key);
			publicKey = keyFactory.generatePublic(publicKeySpec);

			final String subscribe = readString(data);
			subscribePermissions = commaSeperateStringToList(subscribe);
			final String publish = readString(data);
			publishPermissions = commaSeperateStringToList(publish);
			final String commentString = readString(data);
			comment = commentString.length() == 0 ? null : commentString;

			final byte[] signature = new byte[SIGNATURE_LENGTH];
			final int readBytes = data.read(signature);
			if (readBytes <= 0) {
				this.signature = null;
			} else if (readBytes != SIGNATURE_LENGTH) {
				throw new EOFException("Premature end of certificate");
			} else {
				this.signature = signature;
			}

		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new CloudEventBusException("Error decoding simple certificate", e);
		}
	}

	public void store(OutputStream out) throws IOException {
		store(out, true);
	}

	private void store(OutputStream out, boolean writeSignature) throws IOException {
		final DataOutputStream data = new DataOutputStream(out);
		data.write(type.ordinal());
		data.writeLong(serialNumber);
		data.writeLong(issuer);
		data.writeLong(expirationDate);
		data.write(publicKey.getEncoded());
		data.write(listToCommaSeparatedString(subscribePermissions).getBytes());
		data.write(0);
		data.write(listToCommaSeparatedString(publishPermissions).getBytes());
		data.write(0);
		data.write((comment == null ? "" : comment).getBytes());
		data.write(0);
		if (writeSignature && signature != null) {
			data.write(signature);
		}
		data.flush();
	}

	public byte[] hash() {
		if (hash != null) {
			return hash.clone();
		}
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			store(out, false);
			final MessageDigest digest = MessageDigest.getInstance("SHA-1");
			hash = digest.digest(out.toByteArray());
			return hash.clone();
		} catch (IOException | NoSuchAlgorithmException e) {
			throw new CloudEventBusException("Error calculating hash of certificate", e);
		}
	}

	private String listToCommaSeparatedString(List<String> list) {
		final StringBuilder buff = new StringBuilder();
		String sep = "";
		for (String s : list) {
		    buff.append(sep);
		    buff.append(s);
		    sep = ",";
		}
		return buff.toString();
	}

	private List<String> commaSeperateStringToList(String subscribe) {
		final String[] parts = subscribe.split("\\s*,\\s*");
		return copyListUnmodifably(Arrays.asList(parts));
	}

	private String readString(DataInputStream data) throws IOException {
		data.mark(MAX_STRING_LENGTH);
		int length = 0;
		while (data.read() > 0) {
			length++;
		}
		data.reset();
		final byte[] string = new byte[length];
		final int bytesRead = data.read(string);
		if (bytesRead != length) {
			throw new IOException("Error reading string from certificate");
		}
		data.read(); // Throw away null terminating byte
		return new String(string);
	}

	private List<String> copyListUnmodifably(List<String> subscribePermissions) {
		return Collections.unmodifiableList(new ArrayList<>(subscribePermissions));
	}

	public Type getType() {
		return type;
	}

	public long getSerialNumber() {
		return serialNumber;
	}

	public long getIssuer() {
		return issuer;
	}

	public long getExpirationDate() {
		return expirationDate;
	}

	public PublicKey getPublicKey() {
		return publicKey;
	}

	public List<String> getSubscribePermissions() {
		return subscribePermissions;
	}

	public List<String> getPublishPermissions() {
		return publishPermissions;
	}

	public String getComment() {
		return comment;
	}

	public byte[] getSignature() {
		return signature.clone();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Certificate that = (Certificate) o;

		if (expirationDate != that.expirationDate) return false;
		if (issuer != that.issuer) return false;
		if (serialNumber != that.serialNumber) return false;
		if (comment != null ? !comment.equals(that.comment) : that.comment != null) return false;
		if (!publicKey.equals(that.publicKey)) return false;
		if (!publishPermissions.equals(that.publishPermissions)) return false;
		if (!Arrays.equals(signature, that.signature)) return false;
		if (!subscribePermissions.equals(that.subscribePermissions)) return false;
		if (type != that.type) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = (int) (serialNumber ^ (serialNumber >>> 32));
		result = 31 * result + (int) (issuer ^ (issuer >>> 32));
		result = 31 * result + (int) (expirationDate ^ (expirationDate >>> 32));
		result = 31 * result + publicKey.hashCode();
		result = 31 * result + subscribePermissions.hashCode();
		result = 31 * result + publishPermissions.hashCode();
		result = 31 * result + (comment != null ? comment.hashCode() : 0);
		result = 31 * result + (signature != null ? Arrays.hashCode(signature) : 0);
		return result;
	}

	/**
	 * Validates that {@code certificate} was signed using this certificate's private key.
	 *
	 * @param certificate the certificate to validate.
	 * @throws CertificateIssuerMismatchException if {@code certificate.getIssuer()} doesn't match this certificates
	 *                                            serial number.
	 * @throws CertificateSecurityException if a {@link GeneralSecurityException} occurs attempting to validate
	 *                                      {@code certificate}'s signature.
	 * @throws InvalidCertificateSignatureException if {@code certificate}'s signature is invalid.
	 */
	public void validateSignature(Certificate certificate) {
		if (certificate.getIssuer() != serialNumber) {
			throw new CertificateIssuerMismatchException("The issuer of certificate " + certificate.getSerialNumber() + " does not match this certificate.");
		}
		try {
			Cipher cipher = Cipher.getInstance("RSA");
			cipher.init(Cipher.DECRYPT_MODE, publicKey);
			final byte[] decryptedSignature = cipher.doFinal(certificate.getSignature());

			if (!Arrays.equals(certificate.hash(), decryptedSignature)) {
				throw new InvalidCertificateSignatureException("The signature on certificate " + certificate.getSerialNumber() + " is not valid.");
			}
			if (type != Type.AUTHORITY && type != certificate.type) {
				throw new InvalidCertificateException("Certificates of type " + type + " cannot sign certificates of type " + certificate.getType());
			}
		} catch (GeneralSecurityException e) {
			throw new CertificateSecurityException(e);
		}
	}

}
