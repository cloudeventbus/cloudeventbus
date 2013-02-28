package cloudeventbus.server;

import cloudeventbus.Subject;
import cloudeventbus.codec.PublishFrame;
import io.netty.channel.Channel;

import java.net.SocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerPeer implements Peer {

	private final long id;
	private final Channel channel;

	public ServerPeer(long id, Channel channel) {
		this.id = id;
		this.channel = channel;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public SocketAddress getAddress() {
		// TODO We need to get the right remote address from the server.
		return channel.remoteAddress();
	}

	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		channel.write(new PublishFrame(subject, replySubject, body));
	}

	@Override
	public void close() {
		channel.close();
	}

	@Override
	public boolean isConnected() {
		return channel.isActive();
	}
}
