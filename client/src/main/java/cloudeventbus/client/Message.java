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

import java.util.concurrent.TimeUnit;

/**
 * A received message.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Message {

	/**
	 * Returns {@code true} if the message originated from a request, {@code false} if the message originated from a
	 * publish. If the message originated from a request, the {@link #reply(String)} and
	  * {@link #reply(String, long, java.util.concurrent.TimeUnit)} may be used to reply to the request.
	 *
	 * @return {@code true} if the message originated from a request, {@code false} if the message originated from a
	 *         publish.
	 */
	boolean isRequest();

	/**
	 * Returns the subject used to send the message.
	 *
	 * @return the subject used to send the message.
	 */
	String getSubject();

	/**
	 * Returns the body of the message.
	 *
	 * @return the body of the message.
	 */
	String getBody();

	/**
	 * Replies to the message with the specified body. In the case of a
	 * {@link EventBus#request(String, String, MessageHandler, MessageHandler...) request} the reply will be sent to
	 * the client that initiated the request. In the case of a normal publish, the reply will be published over the
	 * same subject used to publish the request. When publishing, the reply subject may be specified using
	 * {@link EventBus#publish(String, String, String)}.
	 *
	 * @param body the body of the response.
	 */
	void reply(String body) throws UnsupportedOperationException;

	/**
	 * Replies to the request with the specified body after a specified delay.
	 *
	 * @see #reply(String) for details on what subject the reply will be published to
	 * @param body the body of the response.
	 * @param delay the amount of time to delay
	 * @param timeUnit the unit of time to delay
	 */
	void reply(String body, long delay, TimeUnit timeUnit) throws UnsupportedOperationException;
}
