/*
 *   Copyright (c) 2013 Mike Heath.  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package cloudeventbus.client;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Collections;
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

	public void addServer(SocketAddress address) {
		synchronized (lock) {
			servers.add(new Server(address));
		}
	}

	public void addServers(Iterable<SocketAddress> addresses) {
		for (SocketAddress address : addresses) {
			addServer(address);
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
				Collections.shuffle(activeServers);
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
