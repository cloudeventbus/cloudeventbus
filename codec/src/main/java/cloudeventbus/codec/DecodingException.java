package cloudeventbus.codec;

import cloudeventbus.CloudEventBusException;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class DecodingException extends CloudEventBusException {
	public DecodingException(String message) {
		super(message);
	}
}
