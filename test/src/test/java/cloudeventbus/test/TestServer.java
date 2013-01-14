package cloudeventbus.test;

import cloudeventbus.Constants;
import cloudeventbus.server.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelStateHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class TestServer implements AutoCloseable {

	final ServerBootstrap bootstrap = new ServerBootstrap();

	private final ChannelGroup channels = new DefaultChannelGroup();

	@ChannelHandler.Sharable
	class ConnectionCounterHandler extends ChannelStateHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext context) throws Exception {
			super.channelActive(context);
			channels.add(context.channel());
		}
	}

	private ConnectionCounterHandler connectionCounter = new ConnectionCounterHandler();

	public TestServer() {
		this("test-server-0.1", Constants.DEFAULT_PORT);
	}

	public TestServer(String agent, int port) {
		bootstrap
				.group(new NioEventLoopGroup(), new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(port))
				.childHandler(new ServerChannelInitializer(agent, null) {
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
