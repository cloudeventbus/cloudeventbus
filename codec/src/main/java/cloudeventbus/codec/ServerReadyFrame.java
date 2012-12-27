package cloudeventbus.codec;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class ServerReadyFrame implements Frame {

	public static final ServerReadyFrame  SERVER_READY = new ServerReadyFrame();

	private ServerReadyFrame() {

	}
}
