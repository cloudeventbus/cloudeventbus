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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CertificateChain extends AbstractList<Certificate> {

	private final List<Certificate> internalList = new ArrayList<>();

	@Override
	public boolean add(Certificate certificate) {
		if (internalList.size() == 0) {
			return internalList.add(certificate);
		} else {
			final Certificate previousCertificate = getLast();
			previousCertificate.validateSignature(certificate);
			for (Certificate c : internalList) {
				if (c.getSerialNumber() == certificate.getSerialNumber()) {
					throw new DuplicateCertificateException("A certificate with the serial number " + certificate.getSerialNumber() + " already exists in the certificate chain.");
				}
			}
		}
		return internalList.add(certificate);
	}

	@Override
	public Certificate get(int index) {
		return internalList.get(index);
	}

	@Override
	public int size() {
		return internalList.size();
	}

	/**
	 * Returns the first certificate in the chain. This certificate is typically signed by an authority.
	 *
	 * @return the first certificate.
	 */
	public Certificate getFirst() {
		return get(0);
	}

	/**
	 * Returns the last certificate in the chain.
	 * @return the last certificate in the chain.
	 */
	public Certificate getLast() {
		return internalList.get(internalList.size() - 1);
	}
}
