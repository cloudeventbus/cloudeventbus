package cloudeventbus.client;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public interface Request extends Iterable<Message>, AutoCloseable {

	@Override
	void close();

	String getSubject();

	int getReceivedMessages();

	Integer getMaxMessages();

	@Override
	SubscriptionIterator iterator();
}
