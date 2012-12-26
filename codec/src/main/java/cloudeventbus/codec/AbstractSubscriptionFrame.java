package cloudeventbus.codec;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AbstractSubscriptionFrame implements Frame {

	private final String subject;

	public AbstractSubscriptionFrame(String subject) {
		this.subject = subject;
	}

	public String getSubject() {
		return subject;
	}
}
