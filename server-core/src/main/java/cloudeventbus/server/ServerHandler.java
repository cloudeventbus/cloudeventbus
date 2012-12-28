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
import cloudeventbus.codec.ServerReadyFrame;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.InvalidSignatureException;
import cloudeventbus.pki.TrustStore;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundMessageHandlerAdapter;
import io.netty.handler.codec.DecoderException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerHandler extends ChannelInboundMessageHandlerAdapter<Frame> {

	private final String versionString;
	private final TrustStore trustStore;

	private byte[] challenge;
	private boolean serverReady = false;
	private CertificateChain clientCertificates;

	public ServerHandler(String versionString, TrustStore trustStore) {
		this.versionString = versionString;
		this.trustStore = trustStore;
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, Frame frame) throws Exception {
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
		} else if (!serverReady) {
			throw new ServerNotReadyException("This server requires authentication.");
		}
		throw new CloudEventBusServerException("Unable to handle frame of type " + frame.getClass().getName());
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.write(new GreetingFrame(versionString));
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
		// TODO Add logging
		if (cause instanceof DecoderException) {
			cause = cause.getCause();
		}
		final ErrorFrame.Code errorCode;
		if (cause instanceof DecodingException) {
			errorCode = ErrorFrame.Code.MALFORMED_REQUEST;
		} else if (cause instanceof InvalidSignatureException) {
			errorCode = ErrorFrame.Code.INVALID_SIGNATURE;
		} else if (cause instanceof ServerNotReadyException) {
			errorCode = ErrorFrame.Code.SERVER_NOT_READY;
		} else {
			errorCode = ErrorFrame.Code.SERVER_ERROR;
		}
		final ErrorFrame errorFrame = new ErrorFrame(errorCode, cause.getMessage());
		ctx.write(errorFrame).addListener(ChannelFutureListener.CLOSE);
	}
}
