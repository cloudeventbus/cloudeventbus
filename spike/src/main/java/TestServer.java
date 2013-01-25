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
import cloudeventbus.Constants;
import cloudeventbus.server.ServerChannelInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class TestServer {
	public static void main(String[] args) {
		final ServerBootstrap bootstrap = new ServerBootstrap();
		bootstrap
				.group(new NioEventLoopGroup(), new NioEventLoopGroup())
				.channel(NioServerSocketChannel.class)
				.localAddress(new InetSocketAddress(Constants.DEFAULT_PORT))
				.childHandler(new ServerChannelInitializer("test-server-0.1", null, null, null))
				.bind().awaitUninterruptibly();
		System.out.println("Server running...");
	}
}
