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
package cloudeventbus.test;

import cloudeventbus.client.Connector;
import cloudeventbus.client.EventBus;
import cloudeventbus.client.Message;
import cloudeventbus.client.MessageHandler;
import cloudeventbus.client.MessageIterator;
import cloudeventbus.client.Request;
import cloudeventbus.client.Subscription;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.*;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class PublishTest {

	@Test
	public void simplePublish() throws Exception {
		try (
				TestServer server = new TestServer()
		) {
			final BlockingConnectionStateListener listener = new BlockingConnectionStateListener(1);
			try (
					EventBus eventBus = new Connector().addServer("localhost").addConnectionStateListener(listener).connect()
			) {
				listener.awaitConnection();
				final String subject = "test";
				final String body = "This is the message body.";
				final Subscription subscription = eventBus.subscribe(subject);
				final MessageIterator iterator = subscription.iterator();
				eventBus.publish(subject, body);
				final Message message = iterator.next(2, TimeUnit.SECONDS);
				assertNotNull(message);
				assertEquals(message.getSubject(), subject);
				assertEquals(message.getBody(), body);
				assertFalse(message.isRequest());

				subscription.close();
				assertFalse(iterator.hasNext());
				assertEquals(subscription.getReceivedMessages(), 1);
			}
		}
	}

	@Test
	public void subscribeClose() throws Exception {
		try (
				TestServer server = new TestServer()
		) {
			final BlockingConnectionStateListener listener = new BlockingConnectionStateListener(1);
			try (
					EventBus eventBus = new Connector().addServer("localhost").addConnectionStateListener(listener).connect()
			) {
				listener.awaitConnection();
				final String subject = "test";
				final String body1 = "This is the message body.";
				final String body2 = "Another message body.";
				final Subscription subscription1 = eventBus.subscribe(subject);
				final Subscription subscription2 = eventBus.subscribe(subject);
				final MessageIterator iterator1 = subscription1.iterator();
				final MessageIterator iterator2 = subscription2.iterator();
				eventBus.publish(subject, body1);

				// Assert first subscription received message
				final Message message1 = iterator1.next(1, TimeUnit.SECONDS);
				assertNotNull(message1);
				assertEquals(message1.getSubject(), subject);
				assertEquals(message1.getBody(), body1);
				assertFalse(message1.isRequest());

				// Assert second subscription received message
				final Message message2 = iterator2.next(1, TimeUnit.SECONDS);
				assertNotNull(message2);
				assertEquals(message2.getSubject(), subject);
				assertEquals(message2.getBody(), body1);
				assertFalse(message2.isRequest());

				// Close second subscription
				subscription2.close();
				assertTrue(iterator1.hasNext());
				assertFalse(iterator2.hasNext());
				assertEquals(subscription1.getReceivedMessages(), 1);
				assertEquals(subscription2.getReceivedMessages(), 1);

				// Publish second message
				eventBus.publish(subject, body2);
				// Make sure first iterator has the recently published message
				final Message message3 = iterator1.next(1, TimeUnit.SECONDS);
				assertNotNull(message3);
				assertEquals(message3.getSubject(), subject);
				assertEquals(message3.getBody(), body2);
			}
		}
	}

	@Test
	public void request() throws Exception {
		try (
				TestServer server = new TestServer()
		) {
			try (
					EventBus eventBus = new Connector().addServer("localhost").connect()
			) {
				final String subject = "test";
				final String body = "This is the message body.";
				final String replyBody = "This is the reply body.";

				eventBus.subscribe(subject).addMessageHandler(new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received request, sending reply.");
						message.reply(replyBody);
					}
				});

				final CountDownLatch latch = new CountDownLatch(1);
				final Request request = eventBus.request(subject, body, new MessageHandler() {
					@Override
					public void onMessage(Message message) {
						System.out.println("Received reply.");
						latch.countDown();
					}
				});

				assertTrue(latch.await(2, TimeUnit.SECONDS));

				assertEquals(request.getReceivedReplies(), 1);
			}
		}
	}

}
