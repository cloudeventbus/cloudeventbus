package cloudeventbus.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class PongFrame implements Frame {

	public static final PongFrame PONG = new PongFrame();

	private PongFrame() {

	}
}
