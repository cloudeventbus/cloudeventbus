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

import cloudeventbus.Constants;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateStoreLoader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.util.CharsetUtil;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayInputStream;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Decoder extends ByteToMessageDecoder<Frame> {

	private final int maxMessageSize;

	protected Decoder() {
		this(Constants.DEFAULT_MAX_MESSAGE_SIZE);
	}

	protected Decoder(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
	}

	@Override
	public Frame decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
		final int frameLength = indexOf(in, Codec.DELIMITER);
		// Frame hasn't been fully read yet.
		if (frameLength < 0) {
			if (in.readableBytes() > maxMessageSize) {
				throw new TooLongFrameException("Frame exceeds maximum size");
			}
			return null;
		}
		// Empty frame, discard and continue decoding
		if (frameLength == 0) {
			in.skipBytes(Codec.DELIMITER.length);
			return decode(ctx, in);
		}
		if (frameLength > maxMessageSize) {
			throw new TooLongFrameException("Frame exceeds maximum size");
		}
		final String command = in.readBytes(frameLength).toString(CharsetUtil.UTF_8);
		in.skipBytes(Codec.DELIMITER.length);
		final String[] parts = command.split("\\s+");
		final char frameType = parts[0].charAt(0);
		final int argumentsLength = parts.length - 1;
		switch (frameType) {
			case FrameTypes.AUTH_RESPONSE:
				assertArgumentsLength(3, argumentsLength, "authentication response");
				final CertificateChain certificates = new CertificateChain();
				final byte[] rawCertificates = Base64.decodeBase64(parts[1].getBytes());
				CertificateStoreLoader.load(new ByteArrayInputStream(rawCertificates), certificates);
				final byte[] salt = Base64.decodeBase64(parts[2]);
				final byte[] digitalSignature = Base64.decodeBase64(parts[3]);
				return new AuthenticationResponseFrame(certificates, salt, digitalSignature);
			case FrameTypes.AUTHENTICATE:
				assertArgumentsLength(1, argumentsLength, "authentication request");
				final byte[] challenge = Base64.decodeBase64(parts[1]);
				return new AuthenticationRequestFrame(challenge);
			case FrameTypes.ERROR:
				if (parts.length == 0) {
					throw new DecodingException("Error is missing error code");
				}
				final Integer errorNumber = Integer.valueOf(parts[1]);
				final ErrorFrame.Code errorCode = ErrorFrame.Code.lookupCode(errorNumber);
				int messageIndex = 1;
				messageIndex = skipWhiteSpace(messageIndex, command);
				while (messageIndex < command.length() && Character.isDigit(command.charAt(messageIndex))) {
					messageIndex++;
				}
				messageIndex = skipWhiteSpace(messageIndex, command);
				final String errorMessage = command.substring(messageIndex).trim();
				if (errorMessage.length() > 0) {
					return new ErrorFrame(errorCode, errorMessage);
				} else {
					return new ErrorFrame(errorCode);
				}
			case FrameTypes.GREETING:
				assertArgumentsLength(1, argumentsLength, "greeting");
				final String version = parts[1];
				return new GreetingFrame(version);
			case FrameTypes.OK:
				return OkFrame.OK;
			case FrameTypes.PING:
				return PingFrame.PING;
			case FrameTypes.PONG:
				return PongFrame.PONG;
			case FrameTypes.PUBLISH:
			case FrameTypes.SEND:
				if (argumentsLength < 2 || argumentsLength > 3) {
					throw new DecodingException("Expected message frame to have 2 or 3 arguments. It has " + argumentsLength + ".");
				}
				final String messageSubject = parts[1];
				final String replySubject;
				final Integer messageLength;
				if (parts.length == 3) {
					replySubject = null;
					messageLength = Integer.valueOf(parts[2]);
				} else {
					replySubject = parts[2];
					messageLength = Integer.valueOf(parts[3]);
				}
				if (in.readableBytes() < messageLength + Codec.DELIMITER.length) {
					// If we haven't received the entire message body (plus the CRLF), wait until it arrives.
					return null;
				}
				final ByteBuf messageBody = in.readBytes(messageLength);
				in.skipBytes(Codec.DELIMITER.length); // Ignore the CRLF after the message body.
				if (frameType == FrameTypes.PUBLISH) {
					return new PublishFrame(messageSubject, replySubject, messageBody);
				} else {
					return new SendFrame(messageSubject, replySubject, messageBody);
				}
			case FrameTypes.SERVER_READY:
				return ServerReadyFrame.SERVER_READY;
			case FrameTypes.SUBSCRIBE:
				assertArgumentsLength(1, argumentsLength, "subscribe");
				return new SubscribeFrame(parts[1]);
			case FrameTypes.UNSUBSCRIBE:
				assertArgumentsLength(1, argumentsLength, "unsubscribe");
				return new UnsubscribeFrame(parts[1]);
			default:
				throw new DecodingException("Unknown frame type " + frameType);
		}
	}

	private int skipWhiteSpace(int messageIndex, String command) {
		while (messageIndex < command.length() && Character.isWhitespace(command.charAt(messageIndex))) {
			messageIndex++;
		}
		return messageIndex;
	}

	private void assertArgumentsLength(int expectedArguments, int argumentsLength, String frameName) {
		if (argumentsLength != expectedArguments) {
			throw new DecodingException("Expected " + frameName + " to have " + expectedArguments + " arguments. It has " + argumentsLength + ".");
		}
	}

	/**
	 * Returns the number of bytes between the readerIndex of the haystack and
	 * the first needle found in the haystack.  -1 is returned if no needle is
	 * found in the haystack.
	 * <p/>
	 * Copied from {@link io.netty.handler.codec.DelimiterBasedFrameDecoder}.
	 */
	private int indexOf(ByteBuf haystack, byte[] needle) {
		for (int i = haystack.readerIndex(); i < haystack.writerIndex(); i++) {
			int haystackIndex = i;
			int needleIndex;
			for (needleIndex = 0; needleIndex < needle.length; needleIndex++) {
				if (haystack.getByte(haystackIndex) != needle[needleIndex]) {
					break;
				} else {
					haystackIndex++;
					if (haystackIndex == haystack.writerIndex() &&
							needleIndex != needle.length - 1) {
						return -1;
					}
				}
			}

			if (needleIndex == needle.length) {
				// Found the needle from the haystack!
				return i - haystack.readerIndex();
			}
		}
		return -1;
	}

}
