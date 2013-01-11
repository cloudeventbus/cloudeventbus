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

import cloudeventbus.Constants;
import cloudeventbus.Subject;
import cloudeventbus.codec.AuthenticationRequestFrame;
import cloudeventbus.codec.AuthenticationResponseFrame;
import cloudeventbus.codec.Codec;
import cloudeventbus.codec.ErrorFrame;
import cloudeventbus.codec.Frame;
import cloudeventbus.codec.GreetingFrame;
import cloudeventbus.codec.PongFrame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.codec.SubscribeFrame;
import cloudeventbus.codec.UnsubscribeFrame;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.InvalidCertificateException;
import cloudeventbus.pki.TrustStore;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
class CloudEventBusImpl implements CloudEventBus {

	private static final Logger LOGGER = LoggerFactory.getLogger(CloudEventBusImpl.class);

	private final ServerList servers = new ServerList();
	private final long reconnectWaitTime;

	private final EventLoopGroup eventLoopGroup;
	private final boolean shutDownEventLoop;
	private final int maxMessageSize;

	private final CertificateChain certificateChain;
	private final PrivateKey privateKey;
	private final TrustStore trustStore;

	private final List<ConnectionStateListener> listeners;

	private final Object lock = new Object();

	// Access to these fields must be synchronized on #lock
	private Channel channel;
	private boolean closed = false;
	private volatile boolean serverReady = false;
	private final Map<Subject, List<AbstractSubscription>> subscriptions = new HashMap<>();

	private volatile CloudEventBusClientException error;

	public CloudEventBusImpl(Connector connector) {
		if (connector.servers.size() == 0) {
			throw new IllegalArgumentException("No servers were specified to connect to.");
		}

		servers.addServers(connector.servers);
		reconnectWaitTime = connector.reconnectWaitTime;

		shutDownEventLoop = connector.eventLoopGroup == null;
		eventLoopGroup =  shutDownEventLoop ? new NioEventLoopGroup() : connector.eventLoopGroup;
		maxMessageSize = connector.maxMessageSize;

		certificateChain = connector.certificateChain;
		privateKey = connector.privateKey;
		trustStore = connector.trustStore;

		listeners = new ArrayList<>(connector.listeners);

		connect();
	}

	private void connect() {
		synchronized (lock) {
			if (closed) {
				return;
			}
		}
		serverReady = false;
		final ServerList.Server server = servers.nextServer();
		LOGGER.debug("Attempting connecting to {}", server.getAddress());
		new Bootstrap()
				.group(eventLoopGroup)
				.remoteAddress(server.getAddress())
				.channel(NioSocketChannel.class)
				.handler(new ClientChannelInitializer())
				.connect().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.isSuccess()) {
					LOGGER.debug("Connection to {} successful", server.getAddress());
					synchronized (lock) {
						channel = future.channel();
						if (closed) {
							channel.close();
						}
					}
				} else {
					LOGGER.warn("Connection to {} failed", server.getAddress());
					synchronized (lock) {
						if (!closed) {
							eventLoopGroup.next().schedule(new Runnable() {
								@Override
								public void run() {
									connect();
								}
							}, reconnectWaitTime, TimeUnit.MILLISECONDS);
						}
					}
				}
			}
		});
	}

	@Override
	public void close() {
		synchronized (lock) {
			closed = true;
			if (shutDownEventLoop) {
				eventLoopGroup.shutdown();
			}
			if (channel != null) {
				channel.close();
			}
		}
		// TODO Close all subscriptions
	}

	@Override
	public boolean isServerReady() {
		return serverReady;
	}

	@Override
	public void publish(String subject, String body) throws ClientClosedException, IllegalArgumentException {
		assertNotClosed();
		// TODO Validate subject is not wildcard
		synchronized (lock) {
			if (channel == null || !channel.isActive()) {
				// TODO Queue up publish
			} else {
				channel.write(new PublishFrame(new Subject(subject), null, body));
			}
		}
	}

	@Override
	public Request request(String subject, String body, MessageHandler replyHandler, MessageHandler... replyHandlers) throws ClientClosedException, IllegalArgumentException {
		return request(subject, body, null, replyHandler, replyHandlers);
	}

	@Override
	public Request request(String subject, String body, Integer maxReplies, MessageHandler replyHandler, MessageHandler... replyHandlers) throws ClientClosedException, IllegalArgumentException {
		assertNotClosed();
		// TODO Validate subject is not wildcard
		synchronized (lock) {
			if (!channel.isActive()) {
				// TODO Queue up request
			} else {
				final Subject replySubject = Subject.createReplySubject();
				// TODO subscribe locally to reply subject

				channel.write(new PublishFrame(new Subject(subject), null, body));
			}
		}
		return null;
	}

	@Override
	public Subscription subscribe(String subject, MessageHandler... messageHandlers) throws ClientClosedException, IllegalArgumentException {
		return subscribe(subject, null, messageHandlers);
	}

	@Override
	public Subscription subscribe(String subject, Integer maxMessages, MessageHandler... messageHandlers) throws ClientClosedException, IllegalArgumentException {
		assertNotClosed();
		final Subject wrappedSubject = new Subject(subject);
		final AbstractSubscription subscription = new AbstractSubscription(subject, maxMessages, messageHandlers) {
			@Override
			public void close() {
				synchronized (lock) {
					final List<AbstractSubscription> subscriptionList = subscriptions.get(wrappedSubject);
					if (subscriptionList.remove(this)) {
						if (subscriptionList.isEmpty() && channel.isActive()) {
							// Send unsubscribe to server if there are no more subscriptions on this subject.
							channel.write(new UnsubscribeFrame(wrappedSubject));
						}
					}
				}
			}
		};

		// Send subscribe to server if this is the first time we're subscribing to this subject.
		if (addSubscription(wrappedSubject, subscription) && channel != null && channel.isActive()) {
			channel.write(new SubscribeFrame(new Subject(subject)));
		}

		return subscription;
	}

	private boolean addSubscription(Subject subject, AbstractSubscription subscription) {
		synchronized (lock) {
			List<AbstractSubscription> subscriptionList = subscriptions.get(subject);
			if (subscriptionList == null) {
				subscriptionList = new ArrayList<>();
				subscriptions.put(subject, subscriptionList);
				subscriptionList.add(subscription);
				return true;
			}
			subscriptionList.add(subscription);
			return false;
		}
	}

	private void assertNotClosed() {
		synchronized (lock) {
			if (closed) {
				throw new ClientClosedException("Client was closed.", error);
			}
		}
	}

	private void fireStateChange(ConnectionStateListener.State state) {
		for (ConnectionStateListener listener : listeners) {
			try {
				listener.onConnectionStateChange(this, state);
			} catch (Throwable t) {
				LOGGER.error("Error invoking connection state listener.", t);
			}
		}
	}

	private class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

		private byte[] challenge;

		@Override
		public void initChannel(SocketChannel channel) throws Exception {
			final ChannelPipeline pipeline = channel.pipeline();
			pipeline.addLast("codec", new Codec(maxMessageSize));
			pipeline.addLast("handler", new ChannelInboundMessageHandlerAdapter<Frame>() {
				@Override
				public void messageReceived(ChannelHandlerContext ctx, Frame frame) throws Exception {
					LOGGER.debug("Received frame: {}", frame);
					switch (frame.getFrameType()) {
						case AUTH_RESPONSE: {
							AuthenticationResponseFrame authenticationResponse = (AuthenticationResponseFrame) frame;
							final CertificateChain certificates = authenticationResponse.getCertificates();
							trustStore.validateCertificateChain(certificates);
							if (certificates.getLast().getType() != Certificate.Type.SERVER) {
								throw new InvalidCertificateException("Server sent a non-server certificate.");
							}
							CertificateUtils.validateSignature(
									certificates.getLast().getPublicKey(),
									challenge,
									authenticationResponse.getSalt(),
									authenticationResponse.getDigitalSignature());
							break;
						}
						case AUTHENTICATE: {
							if (certificateChain == null || privateKey == null) {
								close();
								error = new CloudEventBusClientException("Unable to authenticate with server, missing private key or certificate chain");
								throw error;
							}
							final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) frame;
							final byte[] salt = CertificateUtils.generateChallenge();
							final byte[] signature = CertificateUtils.signChallenge(privateKey, authenticationRequest.getChallenge(), salt);
							AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(certificateChain, salt, signature);
							ctx.write(authenticationResponse);
							break;
						}
						case ERROR:
							final ErrorFrame errorFrame = (ErrorFrame) frame;
							throw new CloudEventBusClientException("Server error: " + errorFrame.getMessage());
						case GREETING:
							final GreetingFrame greetingFrame = (GreetingFrame) frame;
							if (greetingFrame.getVersion() != Constants.PROTOCOL_VERSION) {
								close();
								error = new CloudEventBusClientException("This client does not support protocol version " + greetingFrame.getVersion());
								throw error;
							}
							LOGGER.debug("Received greeting from server {}", ((GreetingFrame) frame).getAgent());
							break;
						case PING:
							LOGGER.debug("Received PING from server, sending PONG.");
							ctx.write(PongFrame.PONG);
							break;
						case PONG:
							LOGGER.debug("Received PONG from server.");
							break;
						case PUBLISH:
							final PublishFrame publishFrame = (PublishFrame) frame;

							synchronized (lock) {
								for (Map.Entry<Subject, List<AbstractSubscription>> entry : subscriptions.entrySet()) {
									final Subject key = entry.getKey();
									final Subject subject = publishFrame.getSubject();
									if (key.isSub(subject)) {
										for (AbstractSubscription subscription : entry.getValue()) {
											subscription.onMessage(
													subject.toString(),
													publishFrame.getBody());
										}
									}
								}
							}
							break;
						case SERVER_READY:
							serverReady = true;
							// Resubscribe with server.
							synchronized (lock) {
								for (Subject subject : subscriptions.keySet()) {
									ctx.write(new SubscribeFrame(subject));
								}
							}
							fireStateChange(ConnectionStateListener.State.SERVERY_READY);
							// TODO publish pending messages
							break;
						default:
							close();
							error = new CloudEventBusClientException("Unable to process command from server " + frame);
							throw error;
					}
				}
				@Override
				public void channelActive(ChannelHandlerContext ctx) throws Exception {
					fireStateChange(ConnectionStateListener.State.CONNECTED);
					ctx.write(new GreetingFrame(Constants.PROTOCOL_VERSION, "test-client-0.1"));
					if (trustStore != null) {
						challenge = CertificateUtils.generateChallenge();
						ctx.write(new AuthenticationRequestFrame(challenge));
					}
				}

				@Override
				public void channelInactive(ChannelHandlerContext ctx) throws Exception {
					fireStateChange(ConnectionStateListener.State.DISCONNECTED);
					serverReady = false;
					synchronized (lock) {
						// If the connection closes unexpectedly, try to immediately reconnect
						if (!closed) {
							connect();
						}
					}
				}
			});
		}

	}
}
