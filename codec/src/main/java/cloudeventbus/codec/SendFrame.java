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
package cloudeventbus.codec;

import cloudeventbus.Subject;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class SendFrame extends AbstractMessageFrame {
	public SendFrame(Subject subject, Subject replySubject, String body) {
		super(subject, replySubject, body);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("Send subject='").append( getSubject()).append("'");
		if (getReplySubject() != null) {
			builder.append(", ").append(getReplySubject()).append("'");
		}
		builder.append(", body='").append(getBody()).append("'");
		return builder.toString();
	}
}
