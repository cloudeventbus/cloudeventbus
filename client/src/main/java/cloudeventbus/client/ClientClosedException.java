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
 * Thrown when a method on a {@link EventBus}, {@link Request}, or {@link Subscription} object is invoked and the
 * object has been closed.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientClosedException extends CloudEventBusClientException {
	public ClientClosedException(String message) {
		super(message);
	}

	public ClientClosedException(String message, Throwable cause) {
		super(message, cause);
	}
}
