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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

/**
 * An {@link Iterator} for iterating over {@link Message} objects.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface MessageIterator extends AutoCloseable, Iterator<Message> {

	/**
	 * Closes the iterator.
	 */
	@Override
	void close();

	/**
	 * Returns {@code true} unless the iterator has been closed. If the {@link Request} or {@link Subscription} from which
	 * this iterator was created are closed, this iterator will be closed automatically.
	 *
	 * @return {@code true} unless the iterator has been closed.
	 */
	@Override
	boolean hasNext();

	/**
	 * Blocks until a message is received. If this iterator is closed while this method blocks, it will throw a
	 * {@link ClientClosedException}.
	 *
	 * @return the received message.
	 * @throws ClientClosedException if the iterator is closed while waiting for a message to arrive.
	 * @throws ClientInterruptedException if the blocked thread is interrupted while waiting for a message.
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	@Override
	Message next() throws ClientClosedException, ClientInterruptedException, NoSuchElementException;

	/**
	 * Throws an {@link UnsupportedOperationException}.
	 *
	 * @throws UnsupportedOperationException it does not even make sense to remove a received message.
	 */
	@Override
	void remove() throws UnsupportedOperationException;

	/**
	 * Blocks until a message is received.
	 *
	 * @return the received message.
	 * @throws ClientClosedException if the iterator has been closed.
	 * @throws ClientInterruptedException if the blocked thread is interrupted while waiting for a message.
	 * @throws NoSuchElementException if the iteration has no more elements
	 */
	Message next(long timeout, TimeUnit unit)
			throws ClientClosedException, ClientInterruptedException, NoSuchElementException;
}
