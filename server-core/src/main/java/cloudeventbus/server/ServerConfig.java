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
package cloudeventbus.server;

import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.TrustStore;

import java.security.PrivateKey;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerConfig {

	private final long id;
	private final int port;
	private final String agentString;
	private final TrustStore trustStore;
	private final CertificateChain certificateChain;
	private final PrivateKey privateKey;

	public ServerConfig(int port, String agentString, TrustStore trustStore, CertificateChain certificateChain, PrivateKey privateKey) {
		this(ThreadLocalRandom.current().nextLong(), port, agentString, trustStore, certificateChain, privateKey);
	}

	public ServerConfig(long id, int port, String agentString, TrustStore trustStore, CertificateChain certificateChain, PrivateKey privateKey) {
		this.id = id;
		this.port = port;
		this.agentString = agentString;
		this.trustStore = trustStore;
		this.certificateChain = certificateChain;
		this.privateKey = privateKey;
	}

	public long getId() {
		return id;
	}

	public int getPort() {
		return port;
	}

	public String getAgentString() {
		return agentString;
	}

	public TrustStore getTrustStore() {
		return trustStore;
	}

	public CertificateChain getCertificateChain() {
		return certificateChain;
	}

	public PrivateKey getPrivateKey() {
		return privateKey;
	}

	public boolean hasSecurityCredentials() {
		return certificateChain != null && privateKey != null;
	}
}
