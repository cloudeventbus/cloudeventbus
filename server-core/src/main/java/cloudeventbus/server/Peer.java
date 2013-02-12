package cloudeventbus.server;

import java.net.SocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Peer {

	long getId();

	SocketAddress getAddress();

}
