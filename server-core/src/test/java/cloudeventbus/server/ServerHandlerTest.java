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
import cloudeventbus.codec.Codec;
import cloudeventbus.codec.ErrorFrame;
import cloudeventbus.codec.GreetingFrame;
import cloudeventbus.codec.ServerReadyFrame;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedByteChannel;
import org.testng.annotations.Test;

import java.security.KeyPair;
import java.util.Arrays;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerHandlerTest {

	@Test
	public void greeting() {
		final String version = "unit-test-server-1.0";
		final EmbeddedByteChannel serverChannel = new EmbeddedByteChannel(new Codec(), new ServerHandler(version, null));
		final EmbeddedByteChannel clientChannel = new EmbeddedByteChannel(new Codec());
		final ByteBuf byteBuf = serverChannel.readOutbound();
		clientChannel.writeInbound(byteBuf);

		// Validate greeting
		final GreetingFrame greeting = (GreetingFrame) clientChannel.readInbound();
		assertNotNull(greeting);
		assertEquals(greeting.getVersion(), version);

		// Validate server ready
		final ServerReadyFrame ready = (ServerReadyFrame) clientChannel.readInbound();
		assertNotNull(ready);
	}

	@Test
	public void authentication() {
		final String version = "test-authentication-server-1.0";

		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final Certificate certificate = CertificateUtils.generateSelfSignedCertificate(keyPair, -1, "Trusted certificate");
		final TrustStore trustStore = new TrustStore(certificate);

		final KeyPair clientKeyPair = CertificateUtils.generateKeyPair();
		final Certificate clientCertificate = CertificateUtils.generateSignedCertificate(
				certificate,
				keyPair.getPrivate(),
				clientKeyPair.getPublic(),
				Certificate.Type.CLIENT,
				-1,
				Arrays.asList("*"),
				Arrays.asList("*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final EmbeddedByteChannel serverChannel = new EmbeddedByteChannel(new Codec(), new ServerHandler(version, trustStore));
		final EmbeddedByteChannel clientChannel = new EmbeddedByteChannel(new Codec());
		ByteBuf byteBuf = serverChannel.readOutbound();
		clientChannel.writeInbound(byteBuf);

		// Validate greeting
		final GreetingFrame greeting = (GreetingFrame) clientChannel.readInbound();
		assertNotNull(greeting);
		assertEquals(greeting.getVersion(), version);

		// Get authentication request
		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) clientChannel.readInbound();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		clientChannel.write(authenticationResponse);
		byteBuf = clientChannel.readOutbound();
		serverChannel.writeInbound(byteBuf);

		// Validate server ready
		byteBuf = serverChannel.readOutbound();
		clientChannel.writeInbound(byteBuf);
		final ServerReadyFrame ready = (ServerReadyFrame) clientChannel.readInbound();
		assertNotNull(ready);
	}

	@Test
	public void badAuthentication() {
		final String version = "test-authentication-server-1.0";

		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final Certificate certificate = CertificateUtils.generateSelfSignedCertificate(keyPair, -1, "Trusted certificate");
		final TrustStore trustStore = new TrustStore(certificate);

		final KeyPair clientKeyPair = CertificateUtils.generateKeyPair();
		final Certificate clientCertificate = CertificateUtils.generateSignedCertificate(
				certificate,
				keyPair.getPrivate(),
				clientKeyPair.getPublic(),
				Certificate.Type.CLIENT,
				-1,
				Arrays.asList("*"),
				Arrays.asList("*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final EmbeddedByteChannel serverChannel = new EmbeddedByteChannel(new Codec(), new ServerHandler(version, trustStore));
		final EmbeddedByteChannel clientChannel = new EmbeddedByteChannel(new Codec());
		ByteBuf byteBuf = serverChannel.readOutbound();
		clientChannel.writeInbound(byteBuf);

		// Validate greeting
		final GreetingFrame greeting = (GreetingFrame) clientChannel.readInbound();
		assertNotNull(greeting);
		assertEquals(greeting.getVersion(), version);

		// Get authentication request
		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) clientChannel.readInbound();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		salt[0]++; // Taint the salt
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		clientChannel.write(authenticationResponse);
		byteBuf = clientChannel.readOutbound();
		serverChannel.writeInbound(byteBuf);

		// Validate server ready
		byteBuf = serverChannel.readOutbound();
		clientChannel.writeInbound(byteBuf);
		final ErrorFrame error = (ErrorFrame) clientChannel.readInbound();
		assertNotNull(error);
	}

}
