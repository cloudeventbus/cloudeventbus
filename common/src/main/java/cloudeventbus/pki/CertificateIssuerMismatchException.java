package cloudeventbus.pki;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateIssuerMismatchException extends CertificateException {
	public CertificateIssuerMismatchException(String message) {
		super(message);
	}
}
