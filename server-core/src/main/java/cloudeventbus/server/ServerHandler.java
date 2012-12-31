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
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.InvalidSignatureException;
import cloudeventbus.pki.TrustStore;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Send ping frame very 30 seconds. Close socket after 1 minute of inactivity.
public class ServerHandler extends ChannelInboundMessageHandlerAdapter<Frame> {

	private static final Logger LOGGER = LoggerFactory.getLogger(ServerHandler.class);

	private static final int SUPPORTED_VERSION = 1;

	private final String agentString;
	private final TrustStore trustStore;

	private byte[] challenge;
	private boolean serverReady = false;
	private CertificateChain clientCertificates;
	private String clientAgent;

	public ServerHandler(String agentString, TrustStore trustStore) {
		this.agentString = agentString;
		this.trustStore = trustStore;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Frame frame) throws Exception {
		// TODO Added toString method to frames for debugging purposes.
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
		}
		// The server has to be "ready" to process frames below this point.
		else if (!serverReady) {
			throw new ServerNotReadyException("This server requires authentication.");
		} else if (frame instanceof PublishFrame) {
			final PublishFrame publishFrame = (PublishFrame) frame;
			// TODO Implement publish
		} else if (frame instanceof SendFrame) {
			final SendFrame sendFrame = (SendFrame) frame;
			// TODO Implement send
		} else if (frame instanceof AuthenticationRequestFrame) {
			// TODO Implement support for the client request authentication
			throw new CloudEventBusServerException("Client to server authentication not yet supported");
		} else if (frame instanceof SubscribeFrame) {
			final SubscribeFrame subscribeFrame = (SubscribeFrame) frame;
			// TODO Implement subscribe
		} else if (frame instanceof UnsubscribeFrame) {
			final UnsubscribeFrame unsubscribeFrame = (UnsubscribeFrame) frame;
			// TODO Implement unsubscribe
		} else if (frame instanceof PingFrame) {
			ctx.write(PongFrame.PONG);
		} else {
			throw new CloudEventBusServerException("Unable to handle frame of type " + frame.getClass().getName());
		}
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.write(new GreetingFrame(SUPPORTED_VERSION, agentString));
		if (trustStore == null) {
			serverReady = true;
			ctx.write(ServerReadyFrame.SERVER_READY);
		} else {
			challenge = CertificateUtils.generateChallenge();
			ctx.write(new AuthenticationRequestFrame(challenge));
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
		} else {
			errorCode = ErrorFrame.Code.SERVER_ERROR;
		}
		final ErrorFrame errorFrame = new ErrorFrame(errorCode, cause.getMessage());
		ctx.write(errorFrame).addListener(ChannelFutureListener.CLOSE);
	}
}
