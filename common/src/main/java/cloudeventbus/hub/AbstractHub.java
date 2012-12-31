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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Wild cards are a pain in the butt!
// TODO Implement send
public abstract class AbstractHub<T> {

	private final Collection<Handler<T>> allWildcardSubscriptions = new CopyOnWriteArrayList<>();
	private final ConcurrentMap<Subject, Collection<Handler<T>>> subscriptions = new ConcurrentHashMap<>();

	public SubscriptionHandle subscribe(Subject subject, final Handler<T> handler) {
		if (subject.isWildCard()) {
			if (subject.equals(Subject.ALL)) {
				return addHandler(handler, allWildcardSubscriptions);
			}
			throw new UnsupportedOperationException("We don't support wild card subjects yet.");
		}
		Collection<Handler<T>> handlers = subscriptions.get(subject);
		if (handlers == null) {
			handlers = new CopyOnWriteArrayList<>();
			final Collection<Handler<T>> existingHandlers = subscriptions.putIfAbsent(subject, handlers);
			// If another thread added a handler collection, use it and discard the collection just created
			if (existingHandlers != null) {
				handlers = existingHandlers;
			}
		}
		return addHandler(handler, handlers);
	}

	private SubscriptionHandle addHandler(final Handler<T> handler, final Collection<Handler<T>> handlers) {
		handlers.add(handler);
		return new SubscriptionHandle() {
			@Override
			public void remove() {
				handlers.remove(handler);
			}
		};
	}

	public void publish(Subject subject, Subject replySubject, String body) {
		final Set<Handler<T>> handlers = new HashSet<>();
		handlers.addAll(allWildcardSubscriptions);
		final Collection<Handler<T>> nonWildCardSubscriptions = subscriptions.get(subject);
		if (nonWildCardSubscriptions != null) {
			handlers.addAll(nonWildCardSubscriptions);
		}

		if (handlers.size() > 0) {
			final T message = encode(subject, replySubject, body);
			for (Handler<T> handler : handlers) {
				handler.publish(message);
			}
		}
	}

	protected abstract T encode(Subject subject, Subject replySubject, String body);

}
