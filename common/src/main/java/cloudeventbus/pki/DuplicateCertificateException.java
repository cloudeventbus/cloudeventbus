package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class DuplicateCertificateException extends CertificateException {
	public DuplicateCertificateException(String message) {
		super(message);
	}
}
