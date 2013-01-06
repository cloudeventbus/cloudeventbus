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
package cloudeventbus.server;

import cloudeventbus.Subject;
import cloudeventbus.codec.Codec;
import cloudeventbus.codec.Frame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.hub.AbstractHub;
import cloudeventbus.hub.Hub;
import cloudeventbus.hub.SubscribeableHub;
import cloudeventbus.pki.TrustStore;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

	private final String versionString;
	private final TrustStore trustStore;
	// TODO Move initialization of Hub out of initializer
	private final SubscribeableHub<Frame> hub = new AbstractHub<Frame>() {
		@Override
		protected Frame encode(Subject subject, Subject replySubject, String body, int recipientCount) {
			return new PublishFrame(subject, replySubject, body);
		}
	};

	public ServerChannelInitializer(String versionString, TrustStore trustStore) {
		this.versionString = versionString;
		this.trustStore = trustStore;
	}

	@Override
	public void initChannel(SocketChannel ch) throws Exception {
		final ChannelPipeline pipeline = ch.pipeline();
		pipeline.addLast(new Codec());
		pipeline.addLast(new ServerHandler(versionString, hub, trustStore));
	}

}
