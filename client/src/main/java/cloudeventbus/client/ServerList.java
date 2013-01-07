package cloudeventbus.client;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Keeps a list of available servers in the cluster.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
class ServerList {

	public static int RECONNECT_LIMIT = 10;

	private final List<Server> servers = new ArrayList<>();

	private final Object lock = new Object();

	private Iterator<Server> iterator;

	public void addServers(Iterable<SocketAddress> addresses) {
		synchronized (lock) {
			for (SocketAddress address : addresses) {
				servers.add(new Server(address));
			}
		}
	}

	public Server nextServer() {
		synchronized (lock) {
			if (servers.size() == 0)  {
				throw new IllegalStateException("No servers in list.");
			}

			if (iterator == null || !iterator.hasNext()) {
				final List<Server> activeServers = new ArrayList<>();
				for (Server server : servers) {
					if (server.connectionAttempts > 0) {
						activeServers.add(server);
					}
				}
				if (activeServers.size() == 0) {
					for (Server server : servers) {
						server.connectionAttempts = RECONNECT_LIMIT;
					}
					activeServers.addAll(servers);
				}
				iterator = activeServers.iterator();
			}
			return iterator.next();
		}
	}

	public class Server {
		private final SocketAddress address;

		private int connectionAttempts = RECONNECT_LIMIT;

		private Server(SocketAddress address) {
			this.address = address;
		}

		public SocketAddress getAddress() {
			return address;
		}

		public void connectionFailure() {
			synchronized (lock) {
				connectionAttempts--;
			}
		}

		public void connectionSuccess() {
			synchronized (lock) {
				connectionAttempts = RECONNECT_LIMIT;
			}
		}
	}

}
