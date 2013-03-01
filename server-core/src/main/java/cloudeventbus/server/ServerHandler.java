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

import cloudeventbus.Constants;
import cloudeventbus.Subject;
import cloudeventbus.codec.AuthenticationRequestFrame;
import cloudeventbus.codec.AuthenticationResponseFrame;
import cloudeventbus.codec.DecodingException;
import cloudeventbus.codec.ErrorFrame;
import cloudeventbus.codec.Frame;
import cloudeventbus.codec.GreetingFrame;
import cloudeventbus.codec.PingFrame;
import cloudeventbus.codec.PongFrame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.codec.ServerReadyFrame;
import cloudeventbus.codec.SubscribeFrame;
import cloudeventbus.codec.UnsubscribeFrame;
import cloudeventbus.hub.SubscribeableHub;
import cloudeventbus.hub.SubscriptionHandle;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificatePermissionError;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.InvalidCertificateException;
import cloudeventbus.pki.InvalidSignatureException;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.channel.EventLoop;
import io.netty.handler.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerHandler extends ChannelInboundMessageHandlerAdapter<Frame> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

	private final ServerConfig serverConfig;

	private final ClusterManager clusterManager;
	private final GlobalHub hub;
	private final SubscribeableHub<Frame> clientSubscriptionHub;

	private byte[] challenge;
	private boolean serverReady = false;
	private CertificateChain clientCertificates;
	private String clientAgent;
	private long clientId;
	private boolean serverConnection;

	// Subscription handler fields
	private NettyHandler handler;
	private final Map<Subject, SubscriptionHandle> subscriptionHandles = new HashMap<>();

	// Ping and idle detection fields
	private Runnable idleTask;
	private ScheduledFuture<?> idleFuture;
	private Runnable pingTask;
	private ScheduledFuture<?> pingFuture;

	public ServerHandler(ServerConfig serverConfig, ClusterManager clusterManager, GlobalHub hub, SubscribeableHub<Frame> clientSubscriptionHub) {
		this.serverConfig = serverConfig;
		this.clusterManager = clusterManager;
		this.hub = hub;
		this.clientSubscriptionHub = clientSubscriptionHub;
	}

	@Override
	public void messageReceived(ChannelHandlerContext context, Frame frame) throws Exception {
		resetIdleTask(context.channel().eventLoop());
		LOGGER.debug("Received frame on server: {}", frame);
		switch (frame.getFrameType()) {
			case AUTH_RESPONSE: {
				AuthenticationResponseFrame authenticationResponse = (AuthenticationResponseFrame) frame;
				final CertificateChain certificates = authenticationResponse.getCertificates();
				serverConfig.getTrustStore().validateCertificateChain(certificates);
				this.clientCertificates = certificates;
				CertificateUtils.validateSignature(
						certificates.getLast().getPublicKey(),
						challenge,
						authenticationResponse.getSalt(),
						authenticationResponse.getDigitalSignature());
				switch (certificates.getLast().getType()) {
					case AUTHORITY:
						throw new InvalidCertificateException("Can not use an authority certificate to authenticate to server.");
					case CLIENT:
						serverConnection = false;
						break;
					case SERVER:
						serverConnection = true;
						clusterManager.addPeer(new ServerPeer(clientId, context.channel()));
						break;
				}
				serverReady = true;
				context.write(ServerReadyFrame.SERVER_READY);
				break;
			}
			case AUTHENTICATE: {
				if (!serverConfig.hasSecurityCredentials()) {
					throw new CloudEventBusServerException("Unable to authenticate with server, missing private key or certificate chain");
				}
				final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) frame;
				final byte[] salt = CertificateUtils.generateChallenge();
				final byte[] signature = CertificateUtils.signChallenge(serverConfig.getPrivateKey(), authenticationRequest.getChallenge(), salt);
				AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(serverConfig.getCertificateChain(), salt, signature);
				context.write(authenticationResponse);
				break;
			}
			case GREETING:
				final GreetingFrame greetingFrame = (GreetingFrame) frame;
				clientAgent = greetingFrame.getAgent();
				clientId = greetingFrame.getId();
				if (greetingFrame.getVersion() != Constants.PROTOCOL_VERSION) {
					throw new InvalidProtocolVersionException("This server doesn't support protocol version " + greetingFrame.getVersion());
				}
				// TODO Try moving this back to channelActive and see if server still crashes...
				context.write(new GreetingFrame(Constants.PROTOCOL_VERSION, serverConfig.getAgentString(), serverConfig.getId()));
				if (serverConfig.getTrustStore() == null) {
					serverReady = true;
					context.write(ServerReadyFrame.SERVER_READY);
				} else {
					challenge = CertificateUtils.generateChallenge();
					context.write(new AuthenticationRequestFrame(challenge));
				}
				break;
			case PONG:
				// Do nothing.
				break;
			default:
				if (!serverReady) {
					throw new ServerNotReadyException("This server requires authentication.");
				} else {
					switch (frame.getFrameType()) {
						case PUBLISH: {
							final PublishFrame publishFrame = (PublishFrame) frame;
							final Subject subject = publishFrame.getSubject();
							final String body = publishFrame.getBody();
							if (clientCertificates != null) {
								clientCertificates.getLast().validatePublishPermission(subject);
							}
							final Subject replySubject = publishFrame.getReplySubject();
							// Implicitly subscribe to request reply subjects
							if (replySubject != null && replySubject.isRequestReply()) {
								clientSubscriptionHub.subscribe(replySubject, handler);
							}
							// If the publish is coming from a peer server, publish locally
							if (serverConnection) {
								hub.publish(subject, replySubject, body);
							} else {
								hub.broadcast(subject, replySubject, body);
							}
							break;
						}
						case SUBSCRIBE: {
							final SubscribeFrame subscribeFrame = (SubscribeFrame) frame;
							final Subject subject = subscribeFrame.getSubject();
							if (clientCertificates != null) {
								clientCertificates.getLast().validateSubscribePermission(subject);
							}
							if (subscriptionHandles.containsKey(subject)) {
								throw new DuplicateSubscriptionException("Already subscribed to subject " + subject);
							}
							// If the connection is a peer server, let the ClusterManager forward messages instead of the normal subscription mechanism
							if (!serverConnection) {
								final SubscriptionHandle subscriptionHandle = clientSubscriptionHub.subscribe(subject, handler);
								subscriptionHandles.put(subject, subscriptionHandle);
							}
							break;
						}
						case UNSUBSCRIBE: {
							final UnsubscribeFrame unsubscribeFrame = (UnsubscribeFrame) frame;
							final Subject subject = unsubscribeFrame.getSubject();
							final SubscriptionHandle subscriptionHandle = subscriptionHandles.get(subject);
							if (subscriptionHandle == null) {
								throw new NotSubscribedException("Not subscribed to subject " + subject);
							}
							subscriptionHandle.remove();
							break;
						}
						case PING:
							context.write(PongFrame.PONG);
							break;
						default:
							throw new CloudEventBusServerException("Unable to handle frame of type " + frame.getClass().getName());
					}
				}
		}
	}

	private void resetIdleTask(EventLoop eventLoop) {
		try {
			if (idleFuture != null) {
				idleFuture.cancel(false);
			}
			if (pingFuture != null) {
				pingFuture.cancel(false);
			}
			idleFuture = eventLoop.schedule(idleTask, 1, TimeUnit.MINUTES);
			pingFuture = eventLoop.schedule(pingTask, 30, TimeUnit.SECONDS);
		} catch (UnsupportedOperationException e) {
			// Don't throw an error when running tests.
			LOGGER.warn("Ping and idle close not supported", e);
		}
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		LOGGER.debug("Channel active from {}", ctx.channel().remoteAddress());
		idleTask = new Runnable() {
			@Override
			public void run() {
				LOGGER.warn("Idle connection {}", ctx.channel().remoteAddress());
				error(ctx, new ErrorFrame(ErrorFrame.Code.IDLE_TIMEOUT, "Connection closed for idle timeout"));
			}
		};
		pingTask = new Runnable() {
			@Override
			public void run() {
				ctx.write(PingFrame.PING);
			}
		};
		resetIdleTask(ctx.channel().eventLoop());
		handler = new NettyHandler(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		LOGGER.debug("Channel inactive from {}", ctx.channel().remoteAddress());
		// Cleanup subscriptions in hub
		for (SubscriptionHandle handle : subscriptionHandles.values()) {
			handle.remove();
		}
		// Cancel idle check and ping tasks.
		if (idleFuture != null) {
			idleFuture.cancel(false);
		}
		if (pingFuture != null) {
			pingFuture.cancel(false);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		// TODO Experiment with using a marker to identify the remote agent.
		LOGGER.error((clientAgent == null ? "" : clientAgent + " ") + cause.getMessage(), cause);
		if (cause instanceof DecoderException) {
			cause = cause.getCause();
		}
		//TODO Move error code to CloudEventBus exception
		final ErrorFrame.Code errorCode;
		if (cause instanceof DecodingException) {
			errorCode = ErrorFrame.Code.MALFORMED_REQUEST;
		} else if (cause instanceof InvalidSignatureException) {
			errorCode = ErrorFrame.Code.INVALID_SIGNATURE;
		} else if (cause instanceof ServerNotReadyException) {
			errorCode = ErrorFrame.Code.SERVER_NOT_READY;
		} else if (cause instanceof InvalidCertificateException) {
			errorCode = ErrorFrame.Code.INVALID_CERTIFICATE;
		} else if (cause instanceof InvalidProtocolVersionException) {
			errorCode = ErrorFrame.Code.UNSUPPORTED_PROTOCOL_VERSION;
		} else if (cause instanceof DuplicateSubscriptionException) {
			errorCode = ErrorFrame.Code.DUPLICATE_SUBSCRIPTION;
		} else if (cause instanceof NotSubscribedException) {
			errorCode = ErrorFrame.Code.NOT_SUBSCRIBED;
		} else if (cause instanceof CertificatePermissionError) {
			errorCode = ErrorFrame.Code.INSUFFICIENT_PRIVILEGES;
		} else {
			errorCode = ErrorFrame.Code.SERVER_ERROR;
		}
		final ErrorFrame errorFrame = new ErrorFrame(errorCode, cause.getMessage());
		error(ctx, errorFrame);
	}

	private void error(ChannelHandlerContext ctx, ErrorFrame errorFrame) {
		ctx.write(errorFrame).addListener(ChannelFutureListener.CLOSE);
	}
}
