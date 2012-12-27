package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class UntrustedCertificateException extends CertificateException {
	public UntrustedCertificateException(String message) {
		super(message);
	}
}
