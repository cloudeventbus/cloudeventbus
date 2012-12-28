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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class TrustStore extends AbstractSet<Certificate> {

	private final Map<Long, Certificate> certificates = Collections.synchronizedMap(new HashMap<Long, Certificate>());

	public TrustStore(Certificate... certs) {
		for (Certificate certificate : certs) {
			add(certificate);
		}
	}

	@Override
	public Iterator<Certificate> iterator() {
		return certificates.values().iterator();
	}

	@Override
	public int size() {
		return certificates.size();
	}

	/**
	 * Returns the certificate from the trust store with the specified serial number.
	 *
	 * @param serialNumber the serial number of the certificate
	 * @return the certificate from the trust store with the specified serial number, {@code null} if the trust store
	 *         does not contain a certificate with the specified serial number.
	 */
	public Certificate get(long serialNumber) {
		return certificates.get(serialNumber);
	}

	/**
	 * Adds a certificate to the trust store.
	 *
	 * @param certificate the certificate to add to the trust store.
	 */
	public boolean add(Certificate certificate) {
		if (certificate.getType() != Certificate.Type.AUTHORITY) {
			throw  new InvalidCertificateException("Only certificates with type authority may be used in a trust store.");
		}
		final long serialNumber = certificate.getSerialNumber();
		if (get(serialNumber) != null) {
			throw new DuplicateCertificateException("The trust store already contains a certificate with serial number" + serialNumber);
		}
		certificates.put(serialNumber, certificate);
		return true;
	}

	public void validateCertificate(Certificate certificate) {
		final long issuerSerialNumber = certificate.getIssuer();
		final Certificate issuerCertificate = get(issuerSerialNumber);
		if (issuerCertificate == null) {
			throw new UntrustedCertificateException("This trust store does not contain an authority certificate with the serial number " + issuerCertificate);
		}
		issuerCertificate.validateSignature(certificate);
	}

	public void validateCertificateChain(CertificateChain certificateChain) {
		if (certificateChain.size() == 0) {
			throw new IllegalArgumentException("Cannot validate an empty certificate chain.");
		}
		validateCertificate(certificateChain.getFirst());
	}
}
