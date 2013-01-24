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
package cloudeventbus.cli;

import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import cloudeventbus.server.ServerChannelInitializer;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.socket.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Server {
	public static void main(String[] args) throws Exception {
		final JCommander commander = new JCommander();

		final Options options = new Options();
		commander.addObject(options);

		commander.setProgramName("eventbus-server");

		try {
			commander.parse(args);

			DefaultOptions.setLogLevel(options);

			TrustStore trustStore = CertificateUtils.loadTrustStore(options.trustStore);
			if (trustStore != null && trustStore.size() > 0) {
				System.out.println("Using trust store at: " + options.trustStore);
			} else {
				trustStore = null;
			}

			final int port = options.port;

			final CertificateChain certificateChain;
			final PrivateKey privateKey;
			if (options.certificate == null) {
				certificateChain = null;
				privateKey = null;
			} else {
				certificateChain = CertificateUtils.loadCertificateChain(options.certificate);
				if (options.privateKey == null) {
					System.err.print("You must specify a private key when using a certificate.");
					System.exit(1);
				} else {
					privateKey = CertificateUtils.loadPrivateKey(options.privateKey);
				}
			}
			// TODO Add support for specifying certificate and private key
			// TODO Implement clustering
			new ServerBootstrap()
					.group(new NioEventLoopGroup(), new NioEventLoopGroup())
					.channel(NioServerSocketChannel.class)
					.localAddress(new InetSocketAddress(port))
					.childHandler(new ServerChannelInitializer("eventbus-simple-server", trustStore))
					.bind().awaitUninterruptibly();
			System.out.println("Server listening on port " + port);
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			commander.usage();
			System.exit(1);
		}
	}

	private static class Options extends DefaultOptions {

		@Parameter(names = "-port", description = "The port the server will listen on.")
		int port = 4223;

		@Parameter(names = "-certificate", description = "The file containing the certificate used to identify the server.")
		String certificate;

		@Parameter(names = "-privateKey", description = "The file containing the private key for the server's certificate.")
		String privateKey;

		@Parameter(names = "-peer", description = "A peer server to cluster with (e.g. -peer 10.1.2.3:4321)")
		List<String> peers;
	}
}
