package cloudeventbus.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class OkFrame {

	public static final OkFrame OK = new OkFrame();

	private OkFrame() {
		// This message can be a singleton.
	}
}
