package cloudeventbus.server;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class InvalidProtocolVersionException extends CloudEventBusServerException {
	public InvalidProtocolVersionException(String message) {
		super(message);
	}
}
