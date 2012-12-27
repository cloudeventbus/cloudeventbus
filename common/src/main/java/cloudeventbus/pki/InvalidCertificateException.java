package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class InvalidCertificateException extends CertificateException {
	public InvalidCertificateException(String s) {
		super(s);
	}
}
