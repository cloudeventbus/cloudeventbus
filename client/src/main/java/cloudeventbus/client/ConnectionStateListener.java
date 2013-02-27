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

/**
 * Interface used for listening to server connection state changes.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO We need something for handling a connection failure.
public interface ConnectionStateListener {

	/**
	 * Called when the connection to the server has been established.
	 *
	 * @param eventBus the event bus that has connected to the server.
	 * @param serverInfo information about the server.
	 */
	void onOpen(EventBus eventBus, ServerInfo serverInfo);

	/**
	 * Called when the connection to the server has been closed.
	 *
	 * @param eventBus the event bus that was connected to the server.
	 * @param serverInfo information about the server.
	 */
	void onClose(EventBus eventBus, ServerInfo serverInfo);

}
