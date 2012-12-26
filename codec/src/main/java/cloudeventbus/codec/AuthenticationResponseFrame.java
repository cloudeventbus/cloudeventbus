package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AuthenticationResponseFrame implements Frame {

	/**
	 * The X.509 certificate that identifies the client (Base64 encoded.)
	 */
	private final String certificate;

	/**
	 * Random bytes sent by the client as part of the digital signature (Base64 encoded).
	 */
	private final String salt;

	/**
	 * The digital signature. This consists of the challenge sent by the server in the
	 * {@link AuthenticationRequestFrame} concatenated with the {@code salt} and encrypted with the private key associated
	 * with the public key stored in the {@code certificate}.
	 */
	private final String digitalSignature;

	public AuthenticationResponseFrame(String certificate, String salt, String digitalSignature) {
		this.certificate = certificate;
		this.salt = salt;
		this.digitalSignature = digitalSignature;
	}

	public String getCertificate() {
		return certificate;
	}

	public String getSalt() {
		return salt;
	}

	public String getDigitalSignature() {
		return digitalSignature;
	}
}
