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

import org.testng.annotations.Test;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerListTest {

	@Test
	public void serverRotation() throws Exception {
		final SocketAddress address1 = new InetSocketAddress(1);
		final SocketAddress address2 = new InetSocketAddress(2);
		final SocketAddress address3 = new InetSocketAddress(3);

		final ServerList servers = new ServerList();
		servers.addServer(address1);
		servers.addServer(address2);
		servers.addServer(address3);

		final Set<ServerList.Server> serverSet = new HashSet<>();

		// Each server should be added to the set, don't check for order because server list gets shuffled.
		assertTrue(serverSet.add(servers.nextServer()));
		assertTrue(serverSet.add(servers.nextServer()));
		assertTrue(serverSet.add(servers.nextServer()));

		// Fourth call should already be in set.
		assertFalse(serverSet.add(servers.nextServer()));

		// Remove a server from the rotation.
		final ServerList.Server serverToRemove1 = removeServerFromRotation(servers);


		serverSet.clear();
		// There should only be two servers in the active rotation.
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		assertEquals(serverSet.size(), 2);

		// Removed server should not be one of them.
		assertTrue(serverSet.add(serverToRemove1));

		// Activate removed server
		serverToRemove1.connectionSuccess();

		serverSet.clear();
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		// There should be 3 servers back in the rotation
		assertEquals(serverSet.size(), 3);

		// Remove two servers from the rotation
		removeServerFromRotation(servers);
		removeServerFromRotation(servers);

		serverSet.clear();
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		serverSet.add(servers.nextServer());
		// There should only be 1 server in the rotation now
		assertEquals(serverSet.size(), 1);

		// Error out last server
		removeServerFromRotation(servers);

		// All three servers should be back in the rotation
		serverSet.clear();
		assertTrue(serverSet.add(servers.nextServer()));
		assertTrue(serverSet.add(servers.nextServer()));
		assertTrue(serverSet.add(servers.nextServer()));
	}

	@Test
	public void addServers() {
		final SocketAddress address1 = new InetSocketAddress(1);
		final SocketAddress address2 = new InetSocketAddress(2);
		final SocketAddress address3 = new InetSocketAddress(3);
		final ServerList servers = new ServerList();
		servers.addServers(Arrays.asList(address1, address2, address3));

		final Set<SocketAddress> addresses = new HashSet<>();
		addresses.add(servers.nextServer().getAddress());
		addresses.add(servers.nextServer().getAddress());
		addresses.add(servers.nextServer().getAddress());

		assertEquals(addresses.size(), 3);
		assertTrue(addresses.contains(address1));
		assertTrue(addresses.contains(address2));
		assertTrue(addresses.contains(address3));
	}

	@Test(expectedExceptions = IllegalStateException.class)
	public void emptyServerListShouldError() {
		new ServerList().nextServer();
	}

	private ServerList.Server removeServerFromRotation(ServerList servers) {
		final ServerList.Server serverToRemove1 = servers.nextServer();
		for (int i = 0; i < ServerList.RECONNECT_LIMIT; i++) {
			serverToRemove1.connectionFailure();
		}
		clearCurrentIterator(servers);
		return serverToRemove1;
	}

	private void clearCurrentIterator(ServerList servers) {
		// Rotate through the list to clear out iterator
		servers.nextServer();
		servers.nextServer();
		servers.nextServer();
		servers.nextServer();
		servers.nextServer();
		servers.nextServer();
	}

}
