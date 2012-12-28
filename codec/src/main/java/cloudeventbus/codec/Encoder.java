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

import cloudeventbus.pki.CertificateStoreLoader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.Base64OutputStream;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Encoder extends MessageToByteEncoder<Frame> {

	@Override
	public void encode(ChannelHandlerContext ctx, Frame frame, ByteBuf out) throws Exception {
		if (frame instanceof AuthenticationRequestFrame) {
			final AuthenticationRequestFrame authenticationRequestFrame = (AuthenticationRequestFrame) frame;
			out.writeByte(FrameTypes.AUTHENTICATE);
			out.writeByte(' ');
			final String challenge = Base64.encodeBase64String(authenticationRequestFrame.getChallenge());
			writeString(out, challenge);
		} else if (frame instanceof AuthenticationResponseFrame) {
			final AuthenticationResponseFrame authenticationResponseFrame = (AuthenticationResponseFrame) frame;
			out.writeByte(FrameTypes.AUTH_RESPONSE);
			out.writeByte(' ');

			// Write certificate chain
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final OutputStream base64Out = new Base64OutputStream(outputStream, true, Integer.MAX_VALUE, new byte[0]);
			CertificateStoreLoader.store(base64Out, authenticationResponseFrame.getCertificates());
			out.writeBytes(outputStream.toByteArray());
			out.writeByte(' ');

			// Write salt
			final byte[] encodedSalt = Base64.encodeBase64(authenticationResponseFrame.getSalt());
			out.writeBytes(encodedSalt);
			out.writeByte(' ');

			// Write signature
			final byte[] encodedDigitalSignature = Base64.encodeBase64(authenticationResponseFrame.getDigitalSignature());
			out.writeBytes(encodedDigitalSignature);
		} else if (frame instanceof ErrorFrame) {
			final ErrorFrame errorFrame = (ErrorFrame) frame;
			out.writeByte(FrameTypes.ERROR);
			out.writeByte(' ');
			writeString(out, Integer.toString(errorFrame.getCode().getErrorNumber()));
			if (errorFrame.getMessage() != null) {
				out.writeByte(' ');
				writeString(out, errorFrame.getMessage());
			}
		} else if (frame instanceof GreetingFrame) {
			final GreetingFrame greetingFrame = (GreetingFrame) frame;
			out.writeByte(FrameTypes.GREETING);
			out.writeByte(' ');
			writeString(out, greetingFrame.getServerVersion());
		} else if (frame instanceof PingFrame) {
			out.writeByte(FrameTypes.PING);
		} else if (frame instanceof PongFrame) {
			out.writeByte(FrameTypes.PONG);
		} else if (frame instanceof PublishFrame) {
			final PublishFrame publishFrame = (PublishFrame) frame;
			out.writeByte(FrameTypes.PUBLISH);
			writeMessageFrame(out, publishFrame);
		} else if (frame instanceof SendFrame) {
			final SendFrame sendFrame = (SendFrame) frame;
			out.writeByte(FrameTypes.SEND);
			writeMessageFrame(out, sendFrame);
		} else if (frame instanceof ServerReadyFrame) {
			out.writeByte(FrameTypes.SERVER_READY);
		} else if (frame instanceof SubscribeFrame) {
			final SubscribeFrame subscribeFrame = (SubscribeFrame) frame;
			out.writeByte(FrameTypes.SUBSCRIBE);
			out.writeByte(' ');
			writeString(out, subscribeFrame.getSubject());
		} else if (frame instanceof UnsubscribeFrame) {
			final UnsubscribeFrame unsubscribeFrame = (UnsubscribeFrame) frame;
			out.writeByte(FrameTypes.UNSUBSCRIBE);
			out.writeByte(' ');
			writeString(out, unsubscribeFrame.getSubject());
		} else {
			throw new EncodingException("Don't know how to encode message of type " + frame.getClass().getName());
		}
		out.writeBytes(Codec.DELIMITER);
	}

	private void writeMessageFrame(ByteBuf out, AbstractMessageFrame publishFrame) {
		out.writeByte(' ');
		writeString(out, publishFrame.getSubject());
		if (publishFrame.getReplySubject() != null) {
			out.writeByte(' ');
			writeString(out, publishFrame.getReplySubject());
		}
		out.writeByte(' ');
		final ByteBuf body = publishFrame.getBody();
		writeString(out, Integer.toString(body.readableBytes()));
		out.writeBytes(Codec.DELIMITER);
		out.writeBytes(body);
		out.writeBytes(Codec.DELIMITER);
	}

	private void writeString(ByteBuf out, String string) {
		out.writeBytes(string.getBytes(CharsetUtil.UTF_8));
	}

}
