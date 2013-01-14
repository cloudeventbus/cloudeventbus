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
package cloudeventbus.test;

import cloudeventbus.client.Connector;
import cloudeventbus.client.EventBus;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ConnectionTest {

	@Test
	public void simpleConnection() throws Exception {
		try (
				TestServer server = new TestServer()
		) {
			assertEquals(server.getConnectionCount(), 0);
			final BlockingConnectionStateListener listener = new BlockingConnectionStateListener();
			try (
					EventBus eventBus = new Connector().addServer("localhost").addConnectionStateListener(listener).connect()
				) {
				listener.awaitReady();
				assertTrue(eventBus.isServerReady());
				assertEquals(server.getConnectionCount(), 1);
				eventBus.close();
				listener.awaitDisconnect();
				assertEquals(server.getConnectionCount(), 0);
			}
		}
	}

	@Test(invocationCount = 4)
	public void reconnect() throws Exception {
		final int port1 = 4223;
		final int port2 = 4224;
		try (
				TestServer server1 = new TestServer("server1", port1);
				TestServer server2 = new TestServer("server2", port2)
		) {
			assertEquals(server1.getConnectionCount(), 0);
			assertEquals(server2.getConnectionCount(), 0);
			final BlockingConnectionStateListener listener1 = new BlockingConnectionStateListener();
			final BlockingConnectionStateListener listener2 = new BlockingConnectionStateListener(2);
			try (
					EventBus eventBus = new Connector()
							.addServer("localhost", port1)
							.addServer("localhost", port2)
							.addConnectionStateListener(listener1)
							.addConnectionStateListener(listener2)
							.connect()
				) {
				listener1.awaitReady();

				final TestServer activeServer;
				final TestServer inactiveServer;
				if (server1.getConnectionCount() == 1) {
					activeServer = server1;
					inactiveServer = server2;
				} else if (server2.getConnectionCount() == 1) {
					activeServer = server2;
					inactiveServer = server1;
				} else {
					fail("Neither server was connected to.");
					return;
				}

				// Shutdown server
				activeServer.close();
				listener1.awaitDisconnect();

				// Await reconnect
				listener2.awaitReady();
				assertEquals(inactiveServer.getConnectionCount(), 1);

				eventBus.close();
				listener2.awaitDisconnect();
				assertEquals(inactiveServer.getConnectionCount(), 0);
			}
		}
	}

	@Test(invocationCount = 4)
	public void invalidHost() throws Exception {
		try (
				TestServer server = new TestServer()
		) {
			assertEquals(server.getConnectionCount(), 0);
			final BlockingConnectionStateListener listener = new BlockingConnectionStateListener();
			try (
					EventBus eventBus = new Connector()
							.addServer("localhost", 4200)
							.addServer("localhost")
							.addConnectionStateListener(listener)
							.connect()
				) {
				listener.awaitReady();
				assertEquals(server.getConnectionCount(), 1);
				eventBus.close();
				listener.awaitDisconnect();
				assertEquals(server.getConnectionCount(), 0);
			}
		}
	}
}
