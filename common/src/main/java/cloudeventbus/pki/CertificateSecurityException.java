package cloudeventbus.pki;

import java.security.GeneralSecurityException;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateSecurityException extends CertificateException {
	public CertificateSecurityException(GeneralSecurityException e) {
		super("Invalid certificate signature", e);
	}
}
