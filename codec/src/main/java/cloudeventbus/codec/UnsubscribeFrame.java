package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class UnsubscribeFrame extends AbstractSubscriptionFrame {
	public UnsubscribeFrame(String subject) {
		super(subject);
	}
}
