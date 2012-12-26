package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class SubscribeFrame extends AbstractSubscriptionFrame {
	public SubscribeFrame(String subject) {
		super(subject);
	}
}
