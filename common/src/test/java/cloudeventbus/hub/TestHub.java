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

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
class TestHub extends AbstractHub<TestHub.Message> {

	static class Message {
		final Subject subject;
		final Subject replySubject;
		final String body;

		Message(Subject subject, Subject replySubject, String body) {
			this.subject = subject;
			this.replySubject = replySubject;
			this.body = body;
		}

		public Subject getSubject() {
			return subject;
		}

		public Subject getReplySubject() {
			return replySubject;
		}

		public String getBody() {
			return body;
		}
	}

	@Override
	protected TestHub.Message encode(Subject subject, Subject replySubject, String body) {
		return new Message(subject, replySubject, body);
	}
}
