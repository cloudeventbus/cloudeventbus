/*
 *   Copyright (c) 2012 Mike Heath.  All rights reserved.
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
package cloudeventbus.hub;

import cloudeventbus.Subject;
import org.testng.annotations.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class HubTest {

	@Test
	public void simpleSubscribe() throws Exception {
		final Subject subject = new Subject("test");
		final String body = "Message body";

		final AtomicBoolean methodCalled = new AtomicBoolean();

		final Hub<TestHub.Message> hub = new TestHub();
		hub.subscribe(new Subject("test"), new Handler<TestHub.Message>() {
			@Override
			public void publish(TestHub.Message message) {
				assertEquals(subject, message.getSubject());
				assertEquals(body, message.getBody());
				methodCalled.set(true);
			}
		});

		hub.publish(subject, null, body);

		assertTrue(methodCalled.get());
	}

	@Test
	public void wildCardSubscribe() {
		final Subject subject = new Subject("test.foo");
		final Subject wildCardSubject = new Subject("test.*");

		final Hub<TestHub.Message> hub = new TestHub();
		final CountHandler handler = new CountHandler();

		hub.subscribe(wildCardSubject, handler);
		hub.publish(subject, null, "Test");

		assertEquals(handler.getCallCount(), 1);
	}

	@Test
	public void wildCardAllSubscribe() throws Exception {
		final Subject subject = new Subject("test");
		final String body = "Message body";

		final AtomicBoolean methodCalled = new AtomicBoolean();

		final Hub<TestHub.Message> hub = new TestHub();
		hub.subscribe(Subject.ALL, new Handler<TestHub.Message>() {
			@Override
			public void publish(TestHub.Message message) {
				assertEquals(subject, message.getSubject());
				assertEquals(body, message.getBody());
				methodCalled.set(true);
			}
		});

		hub.publish(subject, null, body);

		assertTrue(methodCalled.get());
	}

	@Test
	public void handlerCalledOnce() throws Exception {
		final Subject subject = new Subject("test");
		final String body = "Message body";

		final CountHandler handler = new CountHandler();

		final Hub<TestHub.Message> hub = new TestHub();
		hub.subscribe(Subject.ALL, handler);
		hub.subscribe(subject, handler);
		hub.publish(subject, null, body);

		assertEquals(1, handler.getCallCount());
	}

	private class CountHandler implements Handler<TestHub.Message> {

		private int callCount = 0;

		@Override
		public void publish(TestHub.Message message) {
			callCount++;
		}

		public int getCallCount() {
			return callCount;
		}
	}

}
