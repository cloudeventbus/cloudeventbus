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
package cloudeventbus.client;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public interface CloudEventBus  extends AutoCloseable {

	@Override
	void close();

	boolean isConnected();

	void publish(String subject);

	void publish(String subject, String body);

	void publish(String subject, String body, MessageHandler replyHandler, MessageHandler... replyHandlers);

	void publish(String subject, String body, Integer maxReplies, MessageHandler replyHandler, MessageHandler... replyHandlers);

	void send(String subject);

	void send(String subject, String body);

	void send(String subject, String body, MessageHandler replyHandler, MessageHandler... replyHandlers);

	void send(String subject, String body, Integer maxReplies, MessageHandler replyHandler, MessageHandler... replyHandlers);

	Subscription subscribe(String subject);

	Subscription subscribe(String subject, Integer maxMessages);
}
