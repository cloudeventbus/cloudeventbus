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
 * Holds the attributes associated with a subscription. Instances of this class are also {@link Iterable} so they can
 * be used in a {@code for} loop.
 *
 * <p>If the subscription has multiple {@link MessageHandler}s associated with it, each will be invoked when a message
 * is received. Additionally, each iterator created with this subscription will get each message received by this
 * subscription. If an iterator stops being used, its {@link MessageIterator#close()} method should be invoked to keep
 * messages from being queued unnecessarily and wasting memory.
 *
 * @see EventBus#subscribe(String, MessageHandler...)
 * @see EventBus#subscribe(String, Integer, MessageHandler...)
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface Subscription extends Iterable<Message>, AutoCloseable {

	/**
	 * Closes this request. Any {@link MessageHandler} objects associated with this request will no longer receive any
	 * messages after this method is invoked.
	 */
	@Override
	void close();

	/**
	 * Returns the subscription's subject.
	 *
	 * @return the subscription's subject.
	 */
	String getSubject();

	/**
	 * Returns the number of messages this subscriptions has received.
	 *
	 * @return the number of messages this subscriptions has received.
	 */
	int getReceivedMessages();

	/**
	 * Return the maximum number of messages this subscription will receive before being closed automatically.
	 *
	 * @return the maximum number of messages this subscription will receive or {@code null} if no maximum was specified.
	 */
	Integer getMaxMessages();

	/**
	 * Creates a {@link MessageIterator} to iterate over messages received on this subscription.
	 *
	 * @return an iterator.
	 */
	@Override
	MessageIterator iterator();

	/**
	 * Registers a {@link MessageHandler} instance with this subscription that will be invoked every time the
	 * subscription receives a message.
	 *
	 * @param messageHandler the message handler to be invoked
	 * @return a {@code HandlerRegistration} for removing the {@code MessageHandler}.
	 */
	HandlerRegistration addMessageHandler(MessageHandler messageHandler);

}
