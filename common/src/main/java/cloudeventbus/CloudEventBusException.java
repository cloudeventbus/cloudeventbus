package cloudeventbus;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CloudEventBusException extends RuntimeException {
	public CloudEventBusException(String message) {
		super(message);
	}

	public CloudEventBusException(String message, Throwable cause) {
		super(message, cause);
	}
}
