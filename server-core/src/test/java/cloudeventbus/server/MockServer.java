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
package cloudeventbus.server;

import cloudeventbus.Subject;
import cloudeventbus.codec.Codec;
import cloudeventbus.codec.Frame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.hub.AbstractHub;
import cloudeventbus.hub.SubscribeableHub;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.TrustStore;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedByteChannel;

import java.security.PrivateKey;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class MockServer {

	public static final String SERVER_AGENT = "mock-server-0.1";

	final long serverId = ThreadLocalRandom.current().nextLong();
	final EmbeddedByteChannel serverChannel;
	final EmbeddedByteChannel clientChannel = new EmbeddedByteChannel(new Codec());

	final ClusterManager clusterManager;
	final GlobalHub globalHub;

	final SubscribeableHub<Frame> clientSubscriptionHub = new AbstractHub<Frame>() {
		@Override
		protected Frame encode(Subject subject, Subject replySubject, String body, int recipientCount) {
			return new PublishFrame(subject, replySubject, body);
		}
	};

	public MockServer() {
		this(null, null, null);
	}

	public MockServer(TrustStore trustStore, CertificateChain certificateChain, PrivateKey privateKey) {
		globalHub = new GlobalHub();
		clusterManager = new ClusterManager(serverId, globalHub, trustStore, certificateChain, privateKey, null);
		globalHub.addLocalHub(clientSubscriptionHub);
		serverChannel = new EmbeddedByteChannel(
				new Codec(),
				new ServerHandler(
						SERVER_AGENT,
						serverId,
						clusterManager,
						globalHub,
						clientSubscriptionHub,
						trustStore,
						certificateChain,
						privateKey));
	}

	Frame read() {
		final ByteBuf byteBuf = serverChannel.readOutbound();
		if (byteBuf != null) {
			clientChannel.writeInbound(byteBuf);
		}
		return (Frame) clientChannel.readInbound();
	}

	void write(Frame frame) {
		clientChannel.write(frame);
		final ByteBuf byteBuf = clientChannel.readOutbound();
		serverChannel.writeInbound(byteBuf);
	}

	boolean isConnected() {
		return serverChannel.isOpen();
	}
}
