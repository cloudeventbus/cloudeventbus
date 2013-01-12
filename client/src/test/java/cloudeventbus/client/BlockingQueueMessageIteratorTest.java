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
import static org.testng.Assert.*;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class BlockingQueueMessageIteratorTest {

	private static Message EMPTY_MESSAGE = new DefaultMessage("subject", "body", false);

	@Test(expectedExceptions = NoSuchElementException.class)
	public void nextOnClosedIterator() {
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		iterator.close();
		iterator.next();
	}

	@Test(expectedExceptions = NoSuchElementException.class)
	public void nextWithWaitOnClosedIterator() {
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		iterator.close();
		iterator.next(1, TimeUnit.SECONDS);
	}

	@Test
	public void hasNext() {
		final BlockingQueueMessageIterator iterator = new BlockingQueueMessageIterator();
		// Should return true because message may get published
		assertTrue(iterator.hasNext());

		// Queue up a message and close iterator
		iterator.onMessage(EMPTY_MESSAGE);
		iterator.close();

		// Should still return true even though iterator is closed because a message is queued
		assertTrue(iterator.hasNext());

		// Remove message and make sure hasNext is now false
		assertSame(iterator.next(), EMPTY_MESSAGE);
		assertFalse(iterator.hasNext());

		// Calling close a second time shouldn't throw an exception
		iterator.close();
	}

	@Test(expectedExceptions = UnsupportedOperationException.class)
	public void remove() {
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		iterator.remove();
	}

	@Test(expectedExceptions = ClientClosedException.class)
	public void closeWhileBlockingForMessage() {
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new Error(e);
				}
				iterator.close();
			}
		}.start();
		iterator.next();
	}

	@Test(expectedExceptions = ClientClosedException.class)
	public void closeWhileBlockingForMessage2() {
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new Error(e);
				}
				iterator.close();
			}
		}.start();
		iterator.next(5, TimeUnit.SECONDS);
	}

	@Test(expectedExceptions = ClientInterruptedException.class)
	public void InterruptWhileBlockingForMessage() {
		final Thread thread = Thread.currentThread();
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new Error(e);
				}
				thread.interrupt();
			}
		}.start();
		iterator.next();
	}

	@Test(expectedExceptions = ClientInterruptedException.class)
	public void InterruptWhileBlockingForMessage2() {
		final Thread thread = Thread.currentThread();
		final MessageIterator iterator = new BlockingQueueMessageIterator();
		new Thread() {
			@Override
			public void run() {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					throw new Error(e);
				}
				thread.interrupt();
			}
		}.start();
		iterator.next(5, TimeUnit.SECONDS);
	}

	@Test
	public void delayedNext() {
		final BlockingQueueMessageIterator iterator = new BlockingQueueMessageIterator();
		iterator.onMessage(EMPTY_MESSAGE);
		assertSame(iterator.next(1, TimeUnit.SECONDS), EMPTY_MESSAGE);

		final long time = 200;
		final long start = System.currentTimeMillis();
		assertNull(iterator.next(time, TimeUnit.MILLISECONDS));
		final long finish = System.currentTimeMillis();
		// Make sure we're within 50 milliseconds of target time
		assertTrue(finish - start - 200 < 50);
	}
}
