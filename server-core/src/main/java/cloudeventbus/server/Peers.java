package cloudeventbus.server;

import cloudeventbus.Subject;
import cloudeventbus.client.ConnectionStateListener;
import cloudeventbus.client.Connector;
import cloudeventbus.client.EventBus;
import cloudeventbus.client.ServerInfo;
import cloudeventbus.hub.Hub;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.TrustStore;

import java.net.SocketAddress;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Listen for peer changes
// TODO Provide a way to update peer information (so we can get changes via NATS)
// TODO Make sure we're connected to all expected peers
public class Peers implements Hub {

	private final TrustStore trustStore;
	private final CertificateChain serverCertificates;
	private final PrivateKey privateKey;
	private final Hub localHub;

	// Access to peers must by synchronized on #lock
	private final Map<Long, Peer> peers = new HashMap<>();

	private final Object lock = new Object();

	public Peers(TrustStore trustStore, CertificateChain serverCertificates, PrivateKey privateKey, Hub localHub) {
		this.trustStore = trustStore;
		this.serverCertificates = serverCertificates;
		this.privateKey = privateKey;
		this.localHub = localHub;
	}

	/**
	 * Publishes to the local hub.
	 */
	@Override
	public void publish(Subject subject, Subject replySubject, String body) {
		localHub.publish(subject, replySubject, body);
	}

	/**
	 * Broadcasts to the local hub and all peer servers.
	 */
	public void broadcast(Subject subject, Subject replySubject, String body) {
		publish(subject, replySubject, body);
	}

	// TODO The cli server will call this
	// TODO NATS listener will call this
	public void connect(SocketAddress address) {
		synchronized (lock) {
			for (Peer peer : peers.values()) {
				if (peer.getAddress().equals(address)) {
					return;
				}
			}
		}
		final Connector connector = new Connector();
		connector
				.addServer(address)
				.trustStore(trustStore)
				.certificateChain(serverCertificates)
				.privateKey(privateKey)
				.autoReconnect(false) // Set to false so that the client doesn't try connecting to the wrong server.
				.addConnectionStateListener(new ConnectionStateListener() {
					@Override
					public void onOpen(EventBus eventBus, ServerInfo serverInfo) {
						synchronized (lock) {
							// TODO Add/update event bus to peers map
						}
					}

					@Override
					public void onClose(EventBus eventBus, ServerInfo serverInfo) {
						connector.connect();
					}
				})
				.connect();
	}
}
