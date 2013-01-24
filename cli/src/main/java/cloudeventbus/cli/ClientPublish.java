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

import cloudeventbus.client.ConnectionStateListener;
import cloudeventbus.client.Connector;
import cloudeventbus.client.EventBus;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class ClientPublish {

	public static void main(String[] args) throws Exception {
		final JCommander commander = new JCommander();

		final Options options = new Options();
		commander.addObject(options);

		commander.setProgramName("eventbus-publish");

		try {
			commander.parse(args);

			DefaultOptions.setLogLevel(options);

			if (options.mainParameters == null || options.mainParameters.size() != 2) {
				commander.usage();
				System.exit(1);
			}

			final Connector connector = new Connector();
			final TrustStore trustStore = CertificateUtils.loadTrustStore(options.trustStore);
			if (trustStore != null && trustStore.size() > 0) {
				System.out.println("Using trust store at: " + options.trustStore);
				connector.trustStore(trustStore);
			}

			connector.addServer(options.host, options.port);
			if (options.certificate != null) {
				connector.certificateChain(CertificateUtils.loadCertificateChain(options.certificate));
				if (options.privateKey == null) {
					System.err.print("You must specify a private key when using a certificate.");
					System.exit(1);
				} else {
					connector.privateKey(CertificateUtils.loadPrivateKey(options.privateKey));
				}
			}
			connector.addConnectionStateListener(new ConnectionStateListener() {
				@Override
				public void onConnectionStateChange(EventBus eventBus, State state) {
					if (state == State.SERVERY_READY) {
						eventBus.publish(options.mainParameters.get(0), options.mainParameters.get(1));
						eventBus.close();
					}
				}
			}).connect();
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			commander.usage();
			System.exit(1);
		}
	}

	private static class Options extends DefaultOptions {

		@Parameter(description = "subject body")
		List<String> mainParameters;

		@Parameter(names = "-host", description = "Ths host the server is running on.")
		String host = "localhost";

		@Parameter(names = "-port", description = "The port the server will listen on.")
		int port = 4223;

		@Parameter(names = "-certificate", description = "The file containing the certificate used to identify the client.")
		String certificate;

		@Parameter(names = "-privateKey", description = "The file containing the private key for the client's certificate.")
		String privateKey;

	}

}
