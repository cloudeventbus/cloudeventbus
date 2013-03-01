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
import cloudeventbus.codec.ErrorFrame;
import cloudeventbus.codec.GreetingFrame;
import cloudeventbus.codec.PublishFrame;
import cloudeventbus.codec.ServerReadyFrame;
import cloudeventbus.codec.SubscribeFrame;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import org.testng.annotations.Test;

import java.security.KeyPair;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerHandlerTest {

	@Test
	public void greeting() {
		final MockServer server = new MockServer();
		server.write(new GreetingFrame(1, "mock-client", 0L));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final ServerReadyFrame ready = (ServerReadyFrame) server.read();
		assertNotNull(ready);
	}

	@Test
	public void doubleSubscribe() {
		final MockServer server = new MockServer();
		server.write(new GreetingFrame(1, "mock-client", 0l));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final ServerReadyFrame ready = (ServerReadyFrame) server.read();
		assertNotNull(ready);

		server.write(new SubscribeFrame(new Subject("test")));
		assertNull(server.read());

		server.write(new SubscribeFrame(new Subject("test")));
		final ErrorFrame errorFrame = (ErrorFrame) server.read();
		assertNotNull(errorFrame);
		assertEquals(errorFrame.getCode(), ErrorFrame.Code.DUPLICATE_SUBSCRIPTION);
	}

	@Test
	public void authentication() {
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
				Subject.list("*"),
				Subject.list("*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final MockServer server = new MockServer(new ServerConfig(Constants.DEFAULT_PORT, MockServer.SERVER_AGENT, trustStore, null, null));
		server.write(new GreetingFrame(1, "mock-client", 0l));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) server.read();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		server.write(authenticationResponse);

		final ServerReadyFrame ready = (ServerReadyFrame) server.read();
		assertNotNull(ready);
	}

	@Test
	public void badAuthentication() {
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
				Subject.list("*"),
				Subject.list("*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final MockServer server = new MockServer(new ServerConfig(Constants.DEFAULT_PORT, MockServer.SERVER_AGENT, trustStore, null, null));
		server.write(new GreetingFrame(1, "mock-client", 0l));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) server.read();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		salt[0]++; // Taint the salt to create an error condition
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		server.write(authenticationResponse);

		// Validate error
		final ErrorFrame error = (ErrorFrame) server.read();
		assertNotNull(error);
		assertEquals(error.getCode(), ErrorFrame.Code.INVALID_SIGNATURE);
		assertFalse(server.isConnected());
	}

	@Test
	public void publishWithInsufficientPrivileges() {
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
				Subject.list("foo.*"),
				Subject.list("bar.*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final MockServer server = new MockServer(new ServerConfig(Constants.DEFAULT_PORT, MockServer.SERVER_AGENT, trustStore, null, null));
		server.write(new GreetingFrame(1, "mock-client", 0l));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) server.read();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		server.write(authenticationResponse);

		final ServerReadyFrame ready = (ServerReadyFrame) server.read();
		assertNotNull(ready);

		// Successfully authenticated, now publish to a subject we don't have rights to.
		server.write(new PublishFrame(new Subject("test"), null, "This should fail."));

		// Validate error
		final ErrorFrame error = (ErrorFrame) server.read();
		assertNotNull(error);
		assertEquals(error.getCode(), ErrorFrame.Code.INSUFFICIENT_PRIVILEGES);
		assertFalse(server.isConnected());
	}

	@Test
	public void subscribeWithInsufficientPrivileges() {
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
				Subject.list("foo.*"),
				Subject.list("bar.*"),
				"Client certificate for testing"
		);
		final CertificateChain clientCertificates = new CertificateChain(clientCertificate);

		final MockServer server = new MockServer(new ServerConfig(Constants.DEFAULT_PORT, MockServer.SERVER_AGENT, trustStore, null, null));
		server.write(new GreetingFrame(1, "mock-client", 0l));

		final GreetingFrame greeting = (GreetingFrame) server.read();
		assertNotNull(greeting);
		assertEquals(greeting.getAgent(), MockServer.SERVER_AGENT);

		final AuthenticationRequestFrame authenticationRequest = (AuthenticationRequestFrame) server.read();
		assertNotNull(authenticationRequest);

		// Send authentication response
		final byte[] salt = CertificateUtils.generateChallenge();
		final byte[] signature = CertificateUtils.signChallenge(clientKeyPair.getPrivate(), authenticationRequest.getChallenge(), salt);
		AuthenticationResponseFrame authenticationResponse = new AuthenticationResponseFrame(clientCertificates, salt, signature);
		server.write(authenticationResponse);

		final ServerReadyFrame ready = (ServerReadyFrame) server.read();
		assertNotNull(ready);

		// Successfully authenticated, now subscribe to a subject we don't have rights to.
		server.write(new SubscribeFrame(new Subject("test")));

		// Validate error
		final ErrorFrame error = (ErrorFrame) server.read();
		assertNotNull(error);
		assertEquals(error.getCode(), ErrorFrame.Code.INSUFFICIENT_PRIVILEGES);
		assertFalse(server.isConnected());
	}

}
