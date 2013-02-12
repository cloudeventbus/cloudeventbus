import cloudeventbus.client.EventBus;
import cloudeventbus.client.ConnectionStateListener;
import cloudeventbus.client.Connector;
import cloudeventbus.client.Message;
import cloudeventbus.client.MessageHandler;
import cloudeventbus.client.ServerInfo;

import java.net.InetSocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class TestClient {
	public static void main(String[] args) throws Exception {
		final EventBus eventBus = new Connector().addServer(new InetSocketAddress("localhost", 4223))
				.addConnectionStateListener(new ConnectionStateListener() {
					@Override
					public void onOpen(final EventBus eventBus, ServerInfo serverInfo) {
						System.out.println("Server ready.");
					}
					@Override
					public void onClose(EventBus eventBus, ServerInfo serverInfo) {
						System.out.println("Connection closed");
					}
				})
				.connect();
		eventBus.subscribe("help", new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				System.out.println("Received request for help: " + message.getBody());
				message.reply("Does this help?");
			}
		});
		eventBus.request("help", "Help me!", new MessageHandler() {
			@Override
			public void onMessage(Message message) {
				System.out.println("Got help: " + message.getBody());
				eventBus.close();
			}
		});
	}
}
