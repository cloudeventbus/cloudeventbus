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
package cloudeventbus.server;

import cloudeventbus.Subject;
import cloudeventbus.client.EventBus;

import java.net.SocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientPeer implements Peer {

	private final long id;
	private final SocketAddress address;
	private final EventBus eventBus;

	public ClientPeer(long id, SocketAddress address, EventBus eventBus) {
		this.id = id;
		this.address = address;
		this.eventBus = eventBus;
	}

	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		eventBus.publish(subject.toString(), replySubject == null ? null : replySubject.toString(), body);
	}

	@Override
	public boolean isConnected() {
		return eventBus.isServerReady();
	}

	@Override
	public void close() {
		eventBus.close();
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public SocketAddress getAddress() {
		return address;
	}
}
