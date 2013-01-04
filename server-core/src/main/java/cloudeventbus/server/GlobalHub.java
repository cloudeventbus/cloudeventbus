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
import cloudeventbus.hub.Hub;

/**
 * This hub is responsible for distributing messages to all client connections and forwarding messages to peer servers.
 * There should only be one instance of this class per server.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class GlobalHub implements Hub {

	private final Hub clientHub;
	private final Hub peerHub;

	/**
	 * The client hub is the hub that will distribute messages to all client connections including the server itself.
	 * The peer hub is the hub that will distribute messages to all peer servers.
	 *
	 * @param clientHub distributes messages to clients
	 * @param peerHub distributes messages to peer servers
	 */
	public GlobalHub(Hub clientHub, Hub peerHub) {
		this.clientHub = clientHub;
		this.peerHub = peerHub;
	}

	/**
	 * Use this method to distribute messages to the entire distributed system.
	 *
	 * @param subject the message subject
	 * @param replySubject the subject replies should be sent to
	 * @param body the body of the message
	 */
	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		peerHub.publish(subject, replySubject, body);
		clientHub.publish(subject, replySubject, body);
	}

	public Hub getClientHub() {
		return clientHub;
	}

	public Hub getPeerHub() {
		return peerHub;
	}
}
