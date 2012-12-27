package cloudeventbus.pki;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateChain extends AbstractList<Certificate> {

	private final List<Certificate> internalList = new ArrayList<>();

	@Override
	public boolean add(Certificate certificate) {
		if (internalList.size() == 0) {
			return internalList.add(certificate);
		} else {
			final Certificate previousCertificate = internalList.get(internalList.size() - 1);
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
}
