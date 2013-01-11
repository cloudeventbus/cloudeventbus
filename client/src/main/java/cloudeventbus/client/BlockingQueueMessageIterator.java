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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class BlockingQueueMessageIterator implements MessageIterator, MessageHandler {

	private static final Message CLOSED = new Message() {
		@Override
		public boolean isRequest() {
			return false;
		}

		@Override
		public String getSubject() {
			return null;
		}

		@Override
		public String getBody() {
			return null;
		}

		@Override
		public void reply(String body) throws UnsupportedOperationException {
		}

		@Override
		public void reply(String body, long delay, TimeUnit timeUnit) throws UnsupportedOperationException {
		}
	};

	private final BlockingQueue<Message> queue = new LinkedBlockingQueue<>();
	private volatile boolean closed = false;

	@Override
	public void close() {
		if (closed) {
			return;
		}
		closed = true;
		try {
			queue.put(CLOSED);
		} catch (InterruptedException e) {
			throw new ClientInterruptedException(e);
		}
	}

	@Override
	public boolean hasNext() {
		return queue.size() > 0 || !closed;
	}

	@Override
	public Message next() throws ClientClosedException, ClientInterruptedException {
		try {
			final Message message = queue.take();
			if (message == CLOSED) {
				throw new ClientClosedException("Iterator was closed.");
			}
			return message;
		} catch (InterruptedException e) {
			throw new ClientInterruptedException(e);
		}
	}

	@Override
	public Message next(long timeout, TimeUnit unit) throws ClientClosedException, ClientInterruptedException {
		try {
			final Message message = queue.poll(timeout, unit);
			if (message == CLOSED) {
				throw new ClientClosedException("Iterator was closed.");
			}
			return message;
		} catch (InterruptedException e) {
			throw new ClientInterruptedException(e);
		}
	}

	@Override
	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException("You can't remove a message that has been published. Nice try though.");
	}

	@Override
	public void onMessage(Message message) {
		try {
			queue.put(message);
		} catch (InterruptedException e) {
			throw new ClientInterruptedException(e);
		}
	}
}
