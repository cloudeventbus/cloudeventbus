package cloudeventbus.codec;

import cloudeventbus.CloudEventBusException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class EncodingException extends CloudEventBusException {
	public EncodingException(String message) {
		super(message);
	}
}
