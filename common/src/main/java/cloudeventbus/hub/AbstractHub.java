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
public abstract class AbstractHub<T> implements SubscribeableHub<T> {

	private final WildCardNode wildcardSubscriptions = new WildCardNode();
	private final ConcurrentMap<Subject, Collection<Handler<T>>> subscriptions = new ConcurrentHashMap<>();

	@Override
	public SubscriptionHandle subscribe(Subject subject, final Handler<T> handler) {
		if (subject.isWildCard()) {
			WildCardNode currentNode = wildcardSubscriptions;
			for (String part : splitSubject(subject)) {
				if (Subject.WILD_CARD_TOKEN.equals(part)) {
					break;
				}
				currentNode = currentNode.getChild(part, true);
			}
			return addHandler(handler, currentNode.getHandlers());
		}
		return addHandler(handler, getHandlers(subject));
	}

	private String[] splitSubject(Subject subject) {
		return subject.toString().split("\\.");
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

	private Collection<Handler<T>> getHandlers(Subject subject) {
		final Collection<Handler<T>> handlers = subscriptions.get(subject);
		if (handlers == null) {
			final Collection<Handler<T>> newHandlers = new CopyOnWriteArrayList<>();
			final Collection<Handler<T>> existingHandlers = subscriptions.putIfAbsent(subject, newHandlers);
			// If another thread added a handler collection, use it and discard the collection just created
			if (existingHandlers != null) {
				return existingHandlers;
			}
			return newHandlers;
		}
		return handlers;
	}

	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		if (subject.isWildCard()) {
			throw new IllegalArgumentException("Unable to publish to a wildcard subscription.");
		}
		final Set<Handler<T>> handlers = new HashSet<>();

		// Add wildcard handlers
		WildCardNode currentNode = wildcardSubscriptions;
		for (String part : splitSubject(subject)) {
			handlers.addAll(currentNode.getHandlers());
			currentNode = currentNode.getChild(part, false);
			if (currentNode == null) {
				break;
			}
		}

		// Add static handlers
		final Collection<Handler<T>> nonWildCardSubscriptions = subscriptions.get(subject);
		if (nonWildCardSubscriptions != null) {
			handlers.addAll(nonWildCardSubscriptions);
		}

		// If we have any handlers, encode and propagate the message.
		if (handlers.size() > 0) {
			final T message = encode(subject, replySubject, body, handlers.size());
			for (Handler<T> handler : handlers) {
				handler.publish(message);
			}
		}
	}

	protected abstract T encode(Subject subject, Subject replySubject, String body, int recipientCount);

	private class WildCardNode {
		private final Collection<Handler<T>> handlers = new CopyOnWriteArrayList<>();
		private final ConcurrentMap<String, WildCardNode> children = new ConcurrentHashMap<>();

		public Collection<Handler<T>> getHandlers() {
			return handlers;
		}

		public WildCardNode getChild(String nodeName, boolean createIfMissing) {
			final WildCardNode node = children.get(nodeName);
			if (node == null && createIfMissing) {
				final WildCardNode newNode = new WildCardNode();
				final WildCardNode existingNode = children.putIfAbsent(nodeName, newNode);
				if (existingNode != null) {
					return existingNode;
				}
				return newNode;
			}
			return node;
		}
	}
}
