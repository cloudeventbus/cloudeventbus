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
package cloudeventbus.client;

import cloudeventbus.pki.CertificateChain;

import java.net.SocketAddress;

/**
 * Holds information about the server to which the client has connected.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ServerInfo {

	private final SocketAddress serverAddress;
	private final SocketAddress localAddress;
	private final long serverId;
	private final CertificateChain certificateChain;
	private final String serverAgent;

	public ServerInfo(SocketAddress serverAddress, SocketAddress localAddress, long serverId, CertificateChain certificateChain, String serverAgent) {
		this.serverAddress = serverAddress;
		this.localAddress = localAddress;
		this.serverId = serverId;
		this.certificateChain = certificateChain;
		this.serverAgent = serverAgent;
	}

	/**
	 * Returns the server's address.
	 * @return the server's address.
	 */
	public SocketAddress getServerAddress() {
		return serverAddress;
	}

	/**
	 * Returns the client's address.
	 * @return the client's address.
	 */
	public SocketAddress getLocalAddress() {
		return localAddress;
	}

	/**
	 * Returns the server's unique id.
	 * @return the server's unique id
	 */
	public long getServerId() {
		return serverId;
	}

	/**
	 * Returns the server's certificate chain.
	 *
	 * @return the server's certificate chain or {@code null} if the server hasn't been secured.
	 */
	public CertificateChain getServerCertificateChain() {
		return certificateChain;
	}

	/**
	 * The server's agent string.
	 * @return the server's agent string.
	 */
	public String getServerAgent() {
		return serverAgent;
	}
}
