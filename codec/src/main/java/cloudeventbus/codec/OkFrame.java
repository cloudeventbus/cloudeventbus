package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class OkFrame {

	public static final OkFrame OK = new OkFrame();

	private OkFrame() {
		// This message can be a singleton.
	}
}
