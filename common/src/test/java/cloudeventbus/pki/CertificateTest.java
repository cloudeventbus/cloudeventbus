package cloudeventbus.pki;

import static junit.framework.Assert.*;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CertificateTest {

	@Test
	public void createSerializeDeserializeHash() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

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

		assertEquals(certificate, copy);
		assertEquals(certificate.hashCode(), copy.hashCode());
		assertTrue(Arrays.equals(firstHash, copy.hash()));

		assertEquals(type, copy.getType());
		assertEquals(serialNumber, copy.getSerialNumber());
		assertEquals(issuer, copy.getIssuer());
		assertEquals(expirationDate, copy.getExpirationDate());
		assertEquals(keyPair.getPublic(), copy.getPublicKey());
		assertEquals(subscribe, copy.getSubscribePermissions());
		assertEquals(publish, copy.getPublishPermissions());
		assertEquals(comment, copy.getComment());
		assertTrue(Arrays.equals(signature, copy.getSignature()));
	}

}
