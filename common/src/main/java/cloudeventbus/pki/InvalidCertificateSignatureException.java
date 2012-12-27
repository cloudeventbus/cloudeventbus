package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class InvalidCertificateSignatureException extends CertificateException {
	public InvalidCertificateSignatureException(String message) {
		super(message);
	}
}
