package cloudeventbus.test;

import cloudeventbus.Constants;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.TrustStore;
import cloudeventbus.server.ClusterManager;
import cloudeventbus.server.GlobalHub;
import cloudeventbus.server.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelStateHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Write clustering tests
// TODO Write test to verify server id in greeting frame
public class TestServer implements AutoCloseable {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestServer.class);

	private final long id = ThreadLocalRandom.current().nextLong();
	private final ClusterManager clusterManager;
	private final GlobalHub globalHub;

	final ServerBootstrap bootstrap = new ServerBootstrap();

	private final ChannelGroup channels = new DefaultChannelGroup();

	@ChannelHandler.Sharable
	class ConnectionCounterHandler extends ChannelStateHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext context) throws Exception {
			context.channel().closeFuture().addListener(new ChannelFutureListener() {
				@Override
				public void operationComplete(ChannelFuture future) throws Exception {
					LOGGER.debug("Channel closed");
				}
			});
			LOGGER.debug("Adding channel to TestServer group");
			channels.add(context.channel());
			super.channelActive(context);
		}

		@Override
		public void inboundBufferUpdated(ChannelHandlerContext ctx) throws Exception {
			ctx.fireInboundBufferUpdated();
		}
	}

	private ConnectionCounterHandler connectionCounter = new ConnectionCounterHandler();

	public TestServer() {
		this("test-server-0.1", Constants.DEFAULT_PORT);
	}

	public TestServer(String agent, int port) {
		this(agent, port, null, null, null);
	}

	public TestServer(String agent, int port, TrustStore trustStore, CertificateChain certificates, PrivateKey privateKey) {
		final NioEventLoopGroup eventLoopGroup = new NioEventLoopGroup();
		globalHub = new GlobalHub();
		clusterManager = new ClusterManager(id, globalHub, trustStore, certificates, privateKey, eventLoopGroup);
		bootstrap
				.group(eventLoopGroup, new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(port))
				.childHandler(new ServerChannelInitializer(agent, id, clusterManager, globalHub, trustStore, certificates, privateKey) {
					@Override
					public void initChannel(SocketChannel channel) throws Exception {
						super.initChannel(channel);
						final ChannelPipeline pipeline = channel.pipeline();
						pipeline.addFirst("counter", connectionCounter);
					}
				})
				.bind().awaitUninterruptibly();
		System.out.println("Server listening on port " + port);
	}

	public int getConnectionCount() {
		return channels.size();
	}

	@Override
	public void close() {
		bootstrap.shutdown();
	}
}
