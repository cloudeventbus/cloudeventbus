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

import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class DefaultSubscriptionTest {

	@Test
	public void getters() {
		final String subject = "this.is.one.heck.of.a.subject";
		final Integer maxMessages = 3432;
		final DefaultSubscription subscription = new DefaultSubscription(subject, maxMessages);
		assertEquals(subscription.getSubject(), subject);
		assertEquals(subscription.getMaxMessages(), maxMessages);
	}

	@Test
	public void receivedMessageCount() {
		final DefaultSubscription subscription = new DefaultSubscription("subject", 5);
		assertEquals(subscription.getReceivedMessages(), 0);
		subscription.onMessage("subject", null, "body");
		assertEquals(subscription.getReceivedMessages(), 1);
		subscription.onMessage("subject", null, "body");
		assertEquals(subscription.getReceivedMessages(), 2);
	}

	@Test
	public void closeClosesIterator() {
		final DefaultSubscription subscription = new DefaultSubscription("subject", 5);
		final MessageIterator iterator = subscription.iterator();
		assertTrue(iterator.hasNext());
		subscription.close();
		assertFalse(iterator.hasNext());

		// Calling close a second time should not throw exception
		subscription.close();
	}

	@Test
	public void iteratorGetsMessages() {
		final String subject = "some.subject";
		final String replySubject = "reply.subject";
		final String body = "This is a nice body.";
		final DefaultSubscription subscription = new DefaultSubscription("subject", 5);
		final MessageIterator iterator = subscription.iterator();
		subscription.onMessage(subject, replySubject, body);
		final Message message = iterator.next();
		assertNotNull(message);
		assertEquals(message.getSubject(), subject);
		assertEquals(message.getBody(), body);

		// Make sure the iterator doesn't get messages after close is called.
		assertTrue(iterator.hasNext());
		iterator.close();
		subscription.onMessage(subject, replySubject, body);

		assertFalse(iterator.hasNext());
	}

	@Test
	public void handlerGetsMessages() {
		final String subject = "some.subject";
		final String body = "This is a nice body.";
		final DefaultSubscription subscription = new DefaultSubscription("subject", 5);

		final AtomicInteger messageCounter = new AtomicInteger();
		final HandlerRegistration registration = subscription.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				messageCounter.incrementAndGet();
				assertEquals(message.getSubject(), subject);
				assertEquals(message.getBody(), body);
				assertFalse(message.isRequest());
			}
		});
		subscription.onMessage(subject, null, body);
		registration.remove();
		subscription.onMessage(subject, null, body);
		assertEquals(messageCounter.get(), 1);
	}

	@Test(expectedExceptions = ClientClosedException.class)
	public void canNotAddHandlerToClosedSubscription() {
		final DefaultSubscription subscription = new DefaultSubscription("subject", 5);
		subscription.close();
		subscription.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
			}
		});
	}

	@Test(expectedExceptions = ClientClosedException.class)
	public void exceedingMessageCountClosesSubscription() {
		final DefaultSubscription subscription = new DefaultSubscription("subject", 1);

		// Iterator will return true if subscription is open.
		final MessageIterator iterator = subscription.iterator();
		assertTrue(iterator.hasNext());

		subscription.onMessage("subject", null, "body");

		// Get message off iterator queue
		assertNotNull(iterator.next());

		// Iterator should now be closed because subscription will only take one message
		assertFalse(iterator.hasNext());

		// Make sure subscription is closed by adding handler, this will throw exception
		subscription.addMessageHandler(new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				fail("Subscription is closed, exception should have been thrown.");
			}
		});
		subscription.onMessage("subject", null, "body");
	}
}
