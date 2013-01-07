package cloudeventbus.client;

/**
 * Thrown when a method on a {@link CloudEventBus}, {@link Request}, or {@link Subscription} object is invoked and the
 * object has been closed.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientClosedException extends CloudEventBusClientException {

	public ClientClosedException(String message) {
		super(message);
	}
}
