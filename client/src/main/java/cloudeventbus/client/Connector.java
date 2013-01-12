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

import cloudeventbus.Constants;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.TrustStore;
import io.netty.channel.EventLoopGroup;

import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Creates a {@link EventBus} client to connect to a Cloud Event Bus cluster.
 *
 * <p>Each method, except {@code connect()}, returns {@code this} so that the methods may be used for chained invocation. For example,
 *
 * <code>
 *     EventBus eventBus = new Connector().addServer(...).addServer(...).connect();
 * </code>
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Connector {

	/**
	 * The list of candidate servers to connect to.
	 */
	final List<SocketAddress> servers = new ArrayList<>();

	/**
	 * The amount of time to wait before reconnecting to the Cloud Event Bus cluster.
	 */
	long reconnectWaitTime = Constants.DEFAULT_RECONNECT_WAIT_TIME;

	/**
	 * The maximum messages size that can be sent over the network.
	 */
	int maxMessageSize = Constants.DEFAULT_MAX_MESSAGE_SIZE;

	/**
	 * The Netty event loop group to use for connecting to the cluster.
	 */
	EventLoopGroup eventLoopGroup;

	/**
	 * The trust store used for validating the server this client connects to.
	 */
	TrustStore trustStore;

	/**
	 * The certificate chain used to identify this client.
	 */
	CertificateChain certificateChain;

	/**
	 * The private key used to authenticate this client.
	 */
	PrivateKey privateKey;

	/**
	 * The listeners that get invoked when the connection state has changed.
	 */
	final List<ConnectionStateListener> listeners = new ArrayList<>();

	/**
	 * Executor to use for invoking callbacks. By default use same thread.
	 */
	Executor callbackExecutor = new Executor() {
		@Override
		public void execute(Runnable command) {
			command.run();
		}
	};

	/**
	 * Adds a server candidate to connect to. Invoke this method multiple times to add multiple server candidates. One
	 * of the servers will be chosen at random to connect to.
	 *
	 * @param address the address of the server.
	 * @return this connector.
	 */
	public Connector addServer(SocketAddress address) {
		servers.add(address);
		return this;
	}

	/**
	 * Specifies the amount of time to wait before attempting to reconnect to the Cloud Event Bus cluster. The default
	 * is 5 seconds.
	 *
	 * @param time the amount of time to wait
	 * @param timeUnit the time unit of {@code time}
	 * @return this connector.
	 */
	public Connector reconnectWaitTime(long time, TimeUnit timeUnit) {
		this.reconnectWaitTime = timeUnit.toMillis(time);
		return this;
	}

	/**
	 * Specifies the Netty {@code EventLoopGroup} to use for connecting to the Cloud Event Bus cluster. If an event
	 * loop is not specified, one will be created and destroyed when {@link EventBus#close()}
	 * is invoked.
	 *
	 * @param eventLoopGroup the Netty event loop group to use.
	 * @return this connector.
	 */
	public Connector eventLoop(EventLoopGroup eventLoopGroup) {
		this.eventLoopGroup = eventLoopGroup;
		return this;
	}

	/**
	 * Specifies the maximum message size that may be sent over the network. By defaults, this is relatively small to
	 * improve the scalability of the servers. If this value is larger than the value specified by the server this
	 * client connects to, the connection will fail.
	 *
	 * @param maxMessageSize the maximum message size that may be sent over the network in bytes
	 * @return this connector.
	 */
	public Connector maxMessageSize(int maxMessageSize) {
		this.maxMessageSize = maxMessageSize;
		return this;
	}

	/**
	 * Specifies the trust store used for validating the server this client connects to. If a trust store is not
	 * provided, the client will simply not validate any server it connects to.
	 *
	 * @param trustStore the trust store used for validting the server
	 * @return this connector.
	 */
	public Connector trustStore(TrustStore trustStore) {
		this.trustStore = trustStore;
		return this;
	}

	/**
	 * Specifies the certificate chain used to identify this client. If the Cloud Event Bus being connected to does not
	 * require authentication, the certificate chain is not required.
	 *
	 * <p>If a certificate chain is provides, a private key must be provides also, {@link #privateKey(PrivateKey)}.
	 *
	 * @param certificateChain the certificate chain that identifies this client.
	 * @return this connector.
	 */
	public Connector certificateChain(CertificateChain certificateChain) {
		this.certificateChain = certificateChain;
		return this;
	}

	/**
	 * Specifies the private key used to authenticate this client. It is only necessary to provide a private key when a
	 * certificate chain is being provided, {@link #certificateChain(CertificateChain)}.
	 *
	 * @param privateKey the private key used to authenticate this client
	 * @return this connector.
	 */
	public Connector privateKey(PrivateKey privateKey) {
		this.privateKey = privateKey;
		return this;
	}

	/**
	 * Adds a {@link ConnectionStateListener} to the client. This allows you to be notified when a connection is
	 * established, when the server is ready to process messages, and when the connection disconnects. If the
	 * connection to the server closes unexpectedly, the client will automatically try to reconnect to the Cloud
	 * Event Bus cluster.
	 *
	 * @param listener the listener to use
	 * @return this connector.
	 */
	public Connector addConnectionStateListener(ConnectionStateListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * The executor to use for invoking callbacks such as {@link MessageHandler}s and {@link ConnectionStateListener}s.
	 * The default executor uses the Netty IO thread so any blocking in the callback will hold up the client from
	 * publishing or receiving messages.
	 *
	 * @param executor the executor to use for invoking callbacks.
	 * @return this connector.
	 */
	public Connector callbackExecutor(Executor executor) {
		this.callbackExecutor = executor;
		return this;
	}

	/**
	 * Creates a {@link EventBus} instances and initiates a connection to the Cloud Event Bus cluster.
	 *
	 * @return a {@code EventBus} object.
	 */
	public EventBus connect() {
		return new EventBusImpl(this);
	}
}
