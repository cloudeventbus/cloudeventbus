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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This hub is responsible for distributing messages to all client connections and forwarding messages to peer servers.
 * There should only be one instance of this class per server.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class GlobalHub {

	private final List<Hub> localHubs = new CopyOnWriteArrayList<>();
	private final List<Hub> remoteHubs = new CopyOnWriteArrayList<>();

	/**
	 * Use this method to distribute messages to just the local subscribers.
	 *
	 * @param subject the message subject
	 * @param replySubject the subject replies should be sent to
	 * @param body the body of the message
	 */
	public void publish(Subject subject, Subject replySubject, String body) {
		for (Hub hub : localHubs) {
			hub.publish(subject, replySubject, body);
		}
	}

	/**
	 * Use this method to distribute messages locally as well as to peer servers.
	 *
	 * @param subject the message subject
	 * @param replySubject the subject replies should be sent to
	 * @param body the body of the message
	 */
	public void broadcast(Subject subject, Subject replySubject, String body) {
		for (Hub hub : remoteHubs) {
			hub.publish(subject, replySubject, body);
		}
		publish(subject, replySubject, body);
	}

	public void addLocalHub(Hub hub) {
		localHubs.add(hub);
	}

	public void addRemoteHub(Hub hub) {
		remoteHubs.add(hub);
	}
}
