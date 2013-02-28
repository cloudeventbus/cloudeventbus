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

import cloudeventbus.Subject;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelException;
import io.netty.channel.embedded.EmbeddedByteChannel;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.concurrent.ThreadLocalRandom;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class EncoderDecoderTest {


	@Test
	public void authenticationRequest() {
		final byte[] challenge = new byte[] {1,2,3,4,5,6,7,8,9,0};
		final AuthenticationRequestFrame frame = new AuthenticationRequestFrame(challenge);
		final AuthenticationRequestFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getChallenge(), frame.getChallenge());
	}

	@Test
	public void authenticationResponse() {
		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final Certificate certificate = CertificateUtils.generateSelfSignedCertificate(keyPair, -1, "Test Certificate");
		final CertificateChain certificates = new CertificateChain(certificate);
		final byte[] salt = "some salt".getBytes();
		final byte[] signature = "a signature".getBytes();
		final AuthenticationResponseFrame frame = new AuthenticationResponseFrame(certificates, salt, signature);
		final AuthenticationResponseFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getCertificates(), frame.getCertificates());
		assertEquals(recodedFrame.getSalt(), salt);
		assertEquals(recodedFrame.getDigitalSignature(), signature);
	}

	@Test
	public void errorFrame() {
		final ErrorFrame.Code code = ErrorFrame.Code.SERVER_NOT_READY;
		final String message = "It's broken!";
		final ErrorFrame frame = new ErrorFrame(code, message);
		final ErrorFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getCode(), code);
		assertEquals(recodedFrame.getMessage(), message);
	}

	@Test
	public void errorFrameNoMessage() {
		final ErrorFrame.Code code = ErrorFrame.Code.SERVER_NOT_READY;
		final ErrorFrame frame = new ErrorFrame(code);
		final ErrorFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getCode(), code);
		assertNull(recodedFrame.getMessage());
	}

	@Test(expectedExceptions = IllegalArgumentException.class)
	public void errorFrameEmptyMessage() {
		new ErrorFrame(ErrorFrame.Code.SERVER_NOT_READY, " ");
	}

	@Test
	public void greetingFrame() {
		final int version = 1;
		final String agent = "test-0.1-SNAPSHOT";
		final long id = ThreadLocalRandom.current().nextLong();
		final GreetingFrame frame = new GreetingFrame(version, agent, id);
		final GreetingFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getVersion(), version);
		assertEquals(recodedFrame.getAgent(), agent);
		assertEquals(recodedFrame.getId(), id);
	}

	@Test
	public void pingFrame() {
		recode(PingFrame.PING);
	}

	@Test
	public void pongFrame() {
		recode(PongFrame.PONG);
	}

	@Test
	public void serverReadyFrame() {
		recode(ServerReadyFrame.SERVER_READY);
	}

	@Test
	public void publishFrame() {
		final Subject subject = new Subject("test");
		final Subject replySubject = new Subject("_test");
		final String body = "Testing";
		final PublishFrame frame = new PublishFrame(subject, replySubject, body);
		final PublishFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getSubject(), subject);
		assertEquals(recodedFrame.getReplySubject(), replySubject);
		assertEquals(recodedFrame.getBody(), body);
	}

	@Test
	public void publishFrameNoReply() {
		final Subject subject = new Subject("testing.with.no.reply");
		final String body = "I like this body.";
		final PublishFrame frame = new PublishFrame(subject, null, body);
		final PublishFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getSubject(), subject);
		assertNull(recodedFrame.getReplySubject());
		assertEquals(recodedFrame.getBody(), body);
	}

	@Test
	public void subscribe() {
		final Subject subject = new Subject("this.is.some.subject");
		final SubscribeFrame frame = new SubscribeFrame(subject);
		final SubscribeFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getSubject(), subject);
	}

	@Test
	public void unsubscribe() {
		final Subject subject = new Subject("unsubscribe.test");
		final UnsubscribeFrame frame = new UnsubscribeFrame(subject);
		final UnsubscribeFrame recodedFrame = recode(frame);

		assertEquals(recodedFrame.getSubject(), subject);
	}

	@Test(expectedExceptions = ChannelException.class)
	public void unknownFrame() {
		recode(new Frame() {
			@Override
			public FrameType getFrameType() {
				return null;
			}
		});
	}

	private <T extends Frame> T recode(T frame) {
		final EmbeddedByteChannel channel = new EmbeddedByteChannel(new Codec());

		// Encode
		channel.write(frame);
		channel.checkException();
		final ByteBuf data = channel.readOutbound();

		// Decode
		channel.writeInbound(data);
		channel.checkException();
		final T recodedFrame = (T) channel.readInbound();

		// Ensure we got a frame
		assertNotNull(recodedFrame);

		return recodedFrame;
	}

}
