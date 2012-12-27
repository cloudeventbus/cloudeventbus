package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class InvalidCertificateChainException extends CertificateException {
	public InvalidCertificateChainException(String message) {
		super(message);
	}
}
