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

import cloudeventbus.client.Connector;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import com.beust.jcommander.Parameter;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
abstract class AbstractClientOptions  extends DefaultOptions {

	@Parameter(names = "-host", description = "Ths host the server is running on.")
	String host = "localhost";

	@Parameter(names = "-port", description = "The port the server will listen on.")
	int port = 4223;

	@Parameter(names = "-certificate", description = "The file containing the certificate used to identify the client.")
	String certificate;

	@Parameter(names = "-privateKey", description = "The file containing the private key for the client's certificate.")
	String privateKey;

	static Connector configureConnector(AbstractClientOptions options) throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
		final Connector connector = new Connector();
		final TrustStore trustStore = CertificateUtils.loadTrustStore(options.trustStore);
		if (trustStore != null && trustStore.size() > 0) {
			System.out.println("Using trust store at: " + options.trustStore);
			connector.trustStore(trustStore);
		}

		connector.addServer(options.host, options.port);
		if (options.certificate != null) {
			connector.certificateChain(CertificateUtils.loadCertificateChain(options.certificate));
			System.out.println("Using certificate at: " + options.certificate);
			if (options.privateKey == null) {
				System.err.print("You must specify a private key when using a certificate.");
				System.exit(1);
			} else {
				connector.privateKey(CertificateUtils.loadPrivateKey(options.privateKey));
				System.out.println("Using private key at: " + options.privateKey);
			}
		}
		return connector;
	}
}
