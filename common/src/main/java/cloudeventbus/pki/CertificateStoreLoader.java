package cloudeventbus.pki;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public abstract class CertificateStoreLoader {

	public static void load(InputStream in, Collection<Certificate> certificates) throws IOException {
		final DataInputStream data = new DataInputStream(in);
		final int version = data.read();
		if (version != 1) {
			throw new InvalidCertificateException("Expected certificate store with version 1 but is version " + version);
		}
		for(;;) {
			final int size;
			try {
				size = data.readInt();
			} catch (EOFException e) {
				// Break out of loop if we've reached the end of the stream
				break;
			}
			final byte[] rawCertificate = new byte[size];
			data.readFully(rawCertificate);
			final InputStream certificateInputStream = new ByteArrayInputStream(rawCertificate);
			final Certificate certificate = new Certificate(certificateInputStream);
			certificates.add(certificate);
		}
	}

	public static void store(OutputStream out, Iterable<Certificate> certificates) throws IOException {
		final DataOutputStream data = new DataOutputStream(out);
		data.write(1); // Write version
		for (Certificate certificate : certificates) {
			final ByteArrayOutputStream certificateOuputStream = new ByteArrayOutputStream();
			certificate.store(certificateOuputStream);
			final byte[] rawCertificate = certificateOuputStream.toByteArray();
			data.writeInt(rawCertificate.length);
			data.write(rawCertificate);
		}
	}

}
