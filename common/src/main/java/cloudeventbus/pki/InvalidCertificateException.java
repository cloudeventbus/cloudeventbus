package cloudeventbus.pki;

import cloudeventbus.CloudEventBusException;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class InvalidCertificateException extends CloudEventBusException {
	public InvalidCertificateException(String s) {
		super(s);
	}
}
