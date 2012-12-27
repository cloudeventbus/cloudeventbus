package cloudeventbus.pki;

import cloudeventbus.CloudEventBusException;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateException extends CloudEventBusException {
	public CertificateException(String message) {
		super(message);
	}

	public CertificateException(String message, Throwable cause) {
		super(message, cause);
	}
}
