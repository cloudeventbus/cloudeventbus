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
 * A client to a Cloud Event Bus cluster. If the client's connection to the server terminates, the client will
 * automatically try to reconnect to the cluster. The client will obviously not receive any messages when not connected
 * to the cluster but the {@code publish} methods can still be used without throwing an exception. The messages will
 * get published when the client reconnects to the cluster.
 *
 * @see Connector for creating instances of this interface.
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface CloudEventBus  extends AutoCloseable {

	/**
	 * Closes the client.
	 */
	@Override
	void close();

	/**
	 * Indicates if the client is currently connected to a server in a Cloud Event Bus cluster and the server is ready
	 * to process messages.
	 *
	 * @return {@code true} if the client is the remote server in a Cloud Event Bus cluster is ready to process
	 * messages, {@code false} otherwise.
	 */
	boolean isServerReady();

	/**
	 * Publishes a message to the specified subject.
	 *
	 * @param subject the subject on which the message will be published
	 * @param body the body of the message being published
	 * @throws ClientClosedException if this client has been closed.
	 * @throws IllegalArgumentException if the supplied subject contains invalid characters or if the subject is a
	 *                                  wildcard subject.
	 */
	void publish(String subject, String body) throws ClientClosedException, IllegalArgumentException;

	/**
	 * Issues a request to the specified subject expecting a single reply.
	 *
	 * @param subject the subject on which to publish the request
	 * @param body the body of the request
	 * @param replyHandler the first handler for replies
	 * @param replyHandlers additional handlers for replies
	 * @return a {@code Request} object for monitoring the request
	 * @throws ClientClosedException if this client has been closed.
	 * @throws IllegalArgumentException if the supplied subject contains invalid characters or if the subject is a
	 *                                  wildcard subject.
	 */
	Request request(String subject, String body, MessageHandler replyHandler, MessageHandler... replyHandlers) throws ClientClosedException, IllegalArgumentException;

	/**
	 * Issues a request to the specified subject limiting the number of replies the request will receive.
	 *
	 * <p>When the number of message specified by {@code maxReplies} is received, the returned {@link Request} object
	 * will be closed automatically.
	 *
	 * @param subject the subject on which to publish the request
	 * @param body the body of the request
	 * @param maxReplies the maximum number of replies the request will receive
	 * @param replyHandler the first handler for replies
	 * @param replyHandlers additional handlers for replies
	 * @return a {@code Request} object for monitoring the request
	 * @throws ClientClosedException if this client has been closed.
	 * @throws IllegalArgumentException if the supplied subject contains invalid characters or if the subject is a
	 *                                  wildcard subject or if {@code maxReplies} is less than 1.
	 */
	Request request(String subject, String body, Integer maxReplies, MessageHandler replyHandler, MessageHandler... replyHandlers) throws ClientClosedException, IllegalArgumentException;

	/**
	 * Subscribes to the specified subject. The subject may contain a wild card for subscribing to groups of messages.
	 *
	 * @param subject the subject to subscribe to.
	 * @param messageHandlers any {@code MessageHandler}s to be invoked when messages arrive on the subscribe subject
	 * @return a {@code Subscription} object for monitoring the subscription.
	 * @throws ClientClosedException if this client has been closed.
	 * @throws IllegalArgumentException if the supplied subject contains invalid characters.
	 */
	Subscription subscribe(String subject, MessageHandler... messageHandlers) throws ClientClosedException, IllegalArgumentException;

	/**
	 * Subscribes to the specified subject limiting the number of message the subscription will receive. The subject
	 * may contain a wild card for subscribing to groups of messages.
	 *
	 * <p>When the number of message specified by {@code maxMessage} is received, the returned {@link Subscription}
	 * object* will be closed automatically.
	 *
	 * @param subject the subject to subscribe to
	 * @param maxMessages the maximum number of messages the subscription will receive
	 * @param messageHandlers any {@code MessageHandler}s to be invoked when messages arrive on the subscribe subject
	 * @return a {@code Subscription} object for monitoring the subscription.
	 * @throws ClientClosedException if this client has been closed.
	 * @throws IllegalArgumentException if the supplied subject contains invalid characters or if {@code maxMessages}
	 *                                  is less than 1.
	 */
	Subscription subscribe(String subject, Integer maxMessages, MessageHandler... messageHandlers) throws ClientClosedException, IllegalArgumentException;

}
