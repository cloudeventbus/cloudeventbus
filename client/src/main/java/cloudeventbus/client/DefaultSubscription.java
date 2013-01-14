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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class DefaultSubscription implements Subscription {

	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultSubscription.class);

	private final String subject;
	private final Integer maxMessages;
	private final AtomicInteger receivedMessageCount = new AtomicInteger();

	private final List<MessageHandler> handlers = new ArrayList<>();
	private final List<BlockingQueueMessageIterator> iterators = new ArrayList<>();

	private volatile boolean closed;

	public DefaultSubscription(String subject, Integer maxMessages, MessageHandler... messageHandlers) {
		this.subject = subject;
		this.maxMessages = maxMessages;
		Collections.addAll(handlers, messageHandlers);
	}

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		synchronized (iterators) {
			final Iterator<BlockingQueueMessageIterator> messageIteratorIterator = iterators.iterator();
			while (messageIteratorIterator.hasNext()) {
				final BlockingQueueMessageIterator messageIterator = messageIteratorIterator.next();
				messageIteratorIterator.remove();
				messageIterator.close();
			}
		}
	}

	@Override
	public String getSubject() {
		return subject;
	}

	@Override
	public int getReceivedMessages() {
		return receivedMessageCount.get();
	}

	@Override
	public Integer getMaxMessages() {
		return maxMessages;
	}

	@Override
	public MessageIterator iterator() {
		return new BlockingQueueMessageIterator() {
			private final HandlerRegistration registration;
			{
				registration = addMessageHandler(this);
				synchronized (iterators) {
					iterators.add(this);
				}
			}

			@Override
			public void close() {
				registration.remove();
				synchronized (iterators) {
					iterators.remove(this);
				}
				super.close();
			}
		};
	}

	@Override
	public HandlerRegistration addMessageHandler(final MessageHandler messageHandler) {
		if (closed) {
			throw new ClientClosedException("Subscription closed");
		}
		synchronized (handlers) {
			handlers.add(messageHandler);
		}
		return new HandlerRegistration() {
			@Override
			public void remove() {
				synchronized (handlers) {
					handlers.remove(messageHandler);
				}
			}
		};
	}

	public void onMessage(String subject, String replySubject, String body) {
		final int messageCount = receivedMessageCount.incrementAndGet();
		// If the subscription has closed, don't process any late messages.
		if (!closed) {
			final Message message = createMessageObject(subject, replySubject, body);
			synchronized (handlers) {
				for (MessageHandler handler : handlers) {
					try {
						handler.onMessage(message);
					} catch (Throwable t) {
						LOGGER.error("Error in message handler", t);
					}
				}
			}
		}
		if (maxMessages != null && messageCount == maxMessages) {
			close();
		}
	}

	protected DefaultMessage createMessageObject(String subject, String replySubject, String body) {
		return new DefaultMessage(subject, body, replySubject == null);
	}
}
