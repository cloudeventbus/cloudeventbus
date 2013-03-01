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

import cloudeventbus.Subject;
import cloudeventbus.client.ConnectionStateListener;
import cloudeventbus.client.Connector;
import cloudeventbus.client.EventBus;
import cloudeventbus.client.Message;
import cloudeventbus.client.MessageHandler;
import cloudeventbus.client.ServerInfo;
import cloudeventbus.hub.Hub;
import io.netty.channel.EventLoopGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Listen for peer changes
public class ClusterManager implements Hub {

	public static final long CLUSTER_CHECK_INTERVAL = TimeUnit.SECONDS.toMillis(30);
	public static final long MAX_PEER_TRACK_TIME_WITHOUT_CONNECTION = TimeUnit.MINUTES.toMillis(5);

	private static final Logger LOGGER = LoggerFactory.getLogger(ClusterManager.class);

	private final ServerConfig serverConfig;
	private final GlobalHub globalHub;

	private final EventLoopGroup eventLoopGroup;

	// Access to knownPeers must by synchronized on #lock
	private Map<Long, PeerInfo> knownPeers = new HashMap<>();

	private final Object lock = new Object();

	public ClusterManager(final ServerConfig serverConfig,GlobalHub globalHub, EventLoopGroup eventLoopGroup) {
		this.serverConfig = serverConfig;
		this.globalHub = globalHub;
		this.eventLoopGroup = eventLoopGroup;

		globalHub.addRemoteHub(this);

		// Set a peer info for the local server so that it doesn't ever try to connect to itself
		final PeerInfo localPeerInfo = new PeerInfo(new InetSocketAddress(1), true);
		localPeerInfo.setPeer(new Peer() {
			@Override
			public long getId() {
				return serverConfig.getId();
			}

			@Override
			public SocketAddress getAddress() {
				return localPeerInfo.address;
			}

			@Override
			public void publish(Subject subject, Subject replySubject, String body) {
				// Do nothing
			}

			@Override
			public void close() {
				// Do nothing
			}

			@Override
			public boolean isConnected() {
				return true;
			}
		});
		knownPeers.put(serverConfig.getId(), localPeerInfo);

		if (eventLoopGroup != null) {
			eventLoopGroup.next().scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					cloudCheck();
				}
			}, CLUSTER_CHECK_INTERVAL, CLUSTER_CHECK_INTERVAL, TimeUnit.MILLISECONDS);
		}
	}

	/**
	 * Publishes to all the peer servers.
	 */
	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		// TODO Cache the list of known peers so we don't have contention on lock. When a write fails because the peer is closed, update cache. When peer changes in PeerInfo, update cache.
		synchronized (lock) {
			for (PeerInfo peerInfo : knownPeers.values()) {
				if (peerInfo.peer != null && peerInfo.peer.isConnected()) {
					peerInfo.peer.publish(subject, replySubject, body);
				}
			}
		}
	}

	/**
	 * Used for bootstrapping the cluster with known peers.
	 *
	 * @param address the address of the peer server to connect to.
	 */
	public void registerPeer(SocketAddress address) {
		connect(address, true);
	}

	/**
	 * Used for register a peer discovered using an existing service such as NATS.
	 * @param serverId the server's serverId.
	 * @param address  the server's address.
	 */
	public void registerPeer(long serverId, SocketAddress address) {
		final PeerInfo peerInfo = addKnownPeer(serverId, address, false);
		peerInfo.checkConnection();
	}

	public void addPeer(Peer peer) {
		synchronized (lock) {
			addKnownPeer(peer.getId(), peer.getAddress(), false).setPeer(peer);
		}
	}

	public void cloudCheck() {
		LOGGER.debug("Checking status of peer connections");
		synchronized (lock) {
			// Periodically check for peers that need to be connected to
			for (Iterator<PeerInfo> i = knownPeers.values().iterator(); i.hasNext();) {
				final PeerInfo peerInfo = i.next();
				if (peerInfo.peer == null || !peerInfo.peer.isConnected()) {
					final long timeWithoutConnection = System.currentTimeMillis() - peerInfo.timeConnectionUpdated;
					if (timeWithoutConnection > MAX_PEER_TRACK_TIME_WITHOUT_CONNECTION && !peerInfo.staticPeer) {
						LOGGER.info("Unable to connect to peer {}. Removing from cluster.");
						i.remove();
					} else {
						peerInfo.checkConnection();
					}
				}
			}
		}
	}

	private void connect(final SocketAddress address, final boolean staticPeer) {
		LOGGER.debug("Connecting to peer server at {}", address);
		// TODO When we add an option to disable auto discovery of peer servers (when we implement that feature) and disable that here.
		new Connector()
				.addServer(address)
				.trustStore(serverConfig.getTrustStore())
				.certificateChain(serverConfig.getCertificateChain())
				.privateKey(serverConfig.getPrivateKey())
				.eventLoop(eventLoopGroup)
				.uniqueId(serverConfig.getId())
				.addConnectionStateListener(new ConnectionStateListener() {
					private PeerInfo peerInfo;

					@Override
					public void onOpen(EventBus eventBus, ServerInfo serverInfo) {
						final long serverId = serverInfo.getServerId();
						LOGGER.debug("Connected to peer server at {} with id {}", address, serverId);

						eventBus.subscribe(Subject.ALL.toString(), new MessageHandler() {
							@Override
							public void onMessage(Message message) {
								LOGGER.debug("Received message with subject {} from peer server at {}", message.getSubject(), address);
								globalHub.publish(new Subject(message.getSubject()), new Subject(message.getReplySubject()), message.getBody());
							}
						});

						final ClientPeer clientPeer = new ClientPeer(serverId, address, eventBus);
						peerInfo = addKnownPeer(serverId, serverInfo.getLocalAddress(), staticPeer);
						peerInfo.setPeer(clientPeer);
					}

					@Override
					public void onClose(EventBus eventBus, ServerInfo serverInfo) {
						LOGGER.debug("Disconnected from peer server at {} with id {}", address, serverInfo.getServerId());
						// Clean up the client's resources and manage reconnects elsewhere
						eventBus.close();
					}

					@Override
					public void onConnectionFailed(EventBus eventBus) {
						LOGGER.info("Unable to connect to peer server at {}.", address);
						if (!staticPeer) {
							eventBus.close();
						}
					}
				})
				.connect();
	}

	/**
	 * Registers a known peer.
	 */
	private PeerInfo addKnownPeer(long serverId, SocketAddress address, boolean staticPeer) {
		synchronized (lock) {
			PeerInfo peer = knownPeers.get(serverId);
			if (peer == null) {
				peer = new PeerInfo(address, staticPeer);
				knownPeers.put(serverId, peer);
			}
			return peer;
		}
	}

	private class PeerInfo {
		private final SocketAddress address;
		private final boolean staticPeer;

		// Access must be synchronized on ClusterManager#lock
		private Peer peer;
		private long timeConnectionUpdated = System.currentTimeMillis();

		private PeerInfo(SocketAddress address, boolean staticPeer) {
			this.address = address;
			this.staticPeer = staticPeer;
		}

		public void setPeer(Peer peer) {
			synchronized (lock) {
				timeConnectionUpdated = System.currentTimeMillis();
				if (this.peer == null) {
					this.peer = peer;
				} else if (this.peer.isConnected()) {
					// If the current peer is already connected, close the new peer and don't change anything.
					LOGGER.debug("Already connected to peer {}, closing new connection.", peer.getId());
					peer.close();
				} else {
					// If the current peer is closed, replace it with the new peer
					LOGGER.debug("Replacing stale peer new new one {}", peer.getId());
					this.peer.close();
					this.peer = peer;
				}
			}
		}

		public void checkConnection() {
			synchronized (lock) {
				if (peer == null || !peer.isConnected()) {
					LOGGER.debug("Attempting to reconnect to peer {}", address);
					connect(address, staticPeer);
				}
			}
		}
	}
}
