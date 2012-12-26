package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AuthenticationRequestFrame implements Frame {

	/**
	 * Base64 encoded authentication challenge.
	 */
	private final String challenge;

	public AuthenticationRequestFrame(String challenge) {
		this.challenge = challenge;
	}

	public String getChallenge() {
		return challenge;
	}
}
