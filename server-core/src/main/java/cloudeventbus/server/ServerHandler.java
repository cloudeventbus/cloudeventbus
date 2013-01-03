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
import cloudeventbus.codec.AuthenticationRequestFrame;
import cloudeventbus.codec.AuthenticationResponseFrame;
import cloudeventbus.codec.DecodingException;
import cloudeventbus.codec.ErrorFrame;
import cloudeventbus.codec.Frame;
import cloudeventbus.codec.GreetingFrame;
import cloudeventbus.codec.PingFrame;
import cloudeventbus.codec.PongFrame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.codec.SendFrame;
import cloudeventbus.codec.ServerReadyFrame;
import cloudeventbus.codec.SubscribeFrame;
import cloudeventbus.codec.UnsubscribeFrame;
import cloudeventbus.hub.Hub;
import cloudeventbus.hub.SubscriptionHandle;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificatePermissionError;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.InvalidSignatureException;
import cloudeventbus.pki.TrustStore;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerHandler extends ChannelInboundMessageHandlerAdapter<Frame> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

	private static final int SUPPORTED_VERSION = 1;

	private final String agentString;
	private final Hub<PublishFrame> hub;
	private final TrustStore trustStore;
	private final Timer timer;

	private byte[] challenge;
	private boolean serverReady = false;
	private CertificateChain clientCertificates;
	private String clientAgent;
	private NettyHandler handler;
	private final Map<Subject, SubscriptionHandle> subscriptionHandles = new HashMap<>();

	private TimerTask idleTask;
	private Timeout idleTimeout;
	private TimerTask pingTask;
	private Timeout pingTimeout;

	public ServerHandler(String agentString, Hub<PublishFrame> hub, TrustStore trustStore, Timer timer) {
		this.agentString = agentString;
		this.hub = hub;
		this.trustStore = trustStore;
		this.timer = timer;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Frame frame) throws Exception {
		resetIdleTask();
		LOGGER.debug("Received frame: {}", frame);
		if (frame instanceof AuthenticationResponseFrame) {
			AuthenticationResponseFrame authenticationResponse = (AuthenticationResponseFrame) frame;
			final CertificateChain certificates = authenticationResponse.getCertificates();
			trustStore.validateCertificateChain(certificates);
			this.clientCertificates = certificates;
			CertificateUtils.validateSignature(
					certificates.getLast().getPublicKey(),
					challenge,
					authenticationResponse.getSalt(),
					authenticationResponse.getDigitalSignature());
			serverReady = true;
			ctx.write(ServerReadyFrame.SERVER_READY);
		} else if (frame instanceof GreetingFrame) {
			final GreetingFrame greetingFrame = (GreetingFrame) frame;
			clientAgent = greetingFrame.getAgent();
			if (greetingFrame.getVersion() != SUPPORTED_VERSION) {
				throw new InvalidProtocolVersionException("This server doesn't support protocol version " + greetingFrame.getVersion());
			}
		} else if (frame instanceof AuthenticationRequestFrame) {
			// TODO Implement support for the client request authentication
			throw new CloudEventBusServerException("Client to server authentication not yet supported");
		} else if (frame instanceof PongFrame) {
			// Do nothing
		}
		// The server has to be "ready" to process frames below this point.
		else if (!serverReady) {
			throw new ServerNotReadyException("This server requires authentication.");
		} else if (frame instanceof PublishFrame) {
			final PublishFrame publishFrame = (PublishFrame) frame;
			final Subject subject = publishFrame.getSubject();
			if (clientCertificates != null) {
				clientCertificates.getLast().validatePublishPermission(subject);
			}
			hub.publish(subject, publishFrame.getReplySubject(), publishFrame.getBody());
		} else if (frame instanceof SendFrame) {
			final SendFrame sendFrame = (SendFrame) frame;
			if (!hub.send(sendFrame.getSubject(), sendFrame.getReplySubject(), sendFrame.getBody())) {
				// If we can't send the message locally, try sending it to a peer server.
				// TODO Figure out peer servers.
				throw new UnsupportedOperationException("We need to figure out peer servers and how to do a SEND with them.");
			}
		} else if (frame instanceof SubscribeFrame) {
			final SubscribeFrame subscribeFrame = (SubscribeFrame) frame;
			final Subject subject = subscribeFrame.getSubject();
			if (clientCertificates != null) {
				clientCertificates.getLast().validateSubscribePermission(subject);
			}
			if (subscriptionHandles.containsKey(subject)) {
				throw new DuplicateSubscriptionException("Already subscribed to subject " + subject);
			}
			final SubscriptionHandle subscriptionHandle = hub.subscribe(subject, handler);
			subscriptionHandles.put(subject, subscriptionHandle);
		} else if (frame instanceof UnsubscribeFrame) {
			final UnsubscribeFrame unsubscribeFrame = (UnsubscribeFrame) frame;
			final Subject subject = unsubscribeFrame.getSubject();
			final SubscriptionHandle subscriptionHandle = subscriptionHandles.get(subject);
			if (subscriptionHandle == null) {
				throw new NotSubscribedException("Not subscribed to subject " + subject);
			}
			subscriptionHandle.remove();
		} else if (frame instanceof PingFrame) {
			ctx.write(PongFrame.PONG);
		} else {
			throw new CloudEventBusServerException("Unable to handle frame of type " + frame.getClass().getName());
		}
	}

	private void resetIdleTask() {
		if (idleTimeout != null) {
			idleTimeout.cancel();
		}
		if (pingTimeout != null) {
			pingTimeout.cancel();
		}
		idleTimeout = timer.newTimeout(idleTask, 1, TimeUnit.MINUTES);
		pingTimeout = timer.newTimeout(pingTask, 30, TimeUnit.SECONDS);
	}

	@Override
	public void channelActive(final ChannelHandlerContext ctx) throws Exception {
		idleTask = new TimerTask() {
			@Override
			public void run(Timeout timeout) throws Exception {
				LOGGER.warn("Idle connection {}", ctx.channel().remoteAddress());
				error(ctx, new ErrorFrame(ErrorFrame.Code.IDLE_TIMEOUT, "Idle timeout"));
			}
		};
		pingTask = new TimerTask() {
			@Override
			public void run(Timeout timeout) throws Exception {
				ctx.write(PingFrame.PING);
			}
		};
		resetIdleTask();
		ctx.write(new GreetingFrame(SUPPORTED_VERSION, agentString));
		if (trustStore == null) {
			serverReady = true;
			ctx.write(ServerReadyFrame.SERVER_READY);
		} else {
			challenge = CertificateUtils.generateChallenge();
			ctx.write(new AuthenticationRequestFrame(challenge));
		}
		handler = new NettyHandler(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		// Cleanup subscriptions in hub
		for (SubscriptionHandle handle : subscriptionHandles.values()) {
			handle.remove();
		}
		// Cancel idle check and ping tasks.
		if (idleTimeout != null) {
			idleTimeout.cancel();
		}
		if (pingTimeout != null) {
			pingTimeout.cancel();
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
