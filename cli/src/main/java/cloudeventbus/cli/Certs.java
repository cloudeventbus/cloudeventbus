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

import cloudeventbus.Subject;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateChain;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Write and publish docs
public class Certs {
	public static void main(String[] args) throws Exception {
		final JCommander commander = new JCommander();

		final DefaultOptions options = new DefaultOptions();
		final CreateAuthorityCommand createAuthorityCommand = new CreateAuthorityCommand();
		final CreateClientCommand createClientCommand = new CreateClientCommand();
		final CreateServerCommand createServerCommand = new CreateServerCommand();
		final ChainCertificateCommand chainCertificateCommand = new ChainCertificateCommand();
		final ShowCertificateCommand showCertificateCommand = new ShowCertificateCommand();
		final ValidateCommand validateCommand = new ValidateCommand();
		commander.addObject(options);
		commander.addCommand(createAuthorityCommand);
		commander.addCommand(createClientCommand);
		commander.addCommand(createServerCommand);
		commander.addCommand(chainCertificateCommand);
		commander.addCommand(new ListAuthorities());
		commander.addCommand(showCertificateCommand);
		commander.addCommand(validateCommand);

		commander.setProgramName("eventbus-certs");

		try {
			commander.parse(args);

			final String command = commander.getParsedCommand();
			if (command == null) {
				commander.usage();
			} else {
				final TrustStore trustStore = CertificateUtils.loadTrustStore(options.trustStore);
				switch(command) {
					case "create-authority": {
						final KeyPair keyPair = CertificateUtils.generateKeyPair();
						CertificateUtils.savePrivateKey(keyPair.getPrivate(), createAuthorityCommand.privateKey);
						final Certificate certificate = CertificateUtils.generateSelfSignedCertificate(
								keyPair,
								getExpirationDate(createAuthorityCommand.expirationDate),
								Subject.list(createAuthorityCommand.subscribePermissions),
								Subject.list(createAuthorityCommand.publishPermissions),
								createAuthorityCommand.comment
						);
						trustStore.add(certificate);

						CertificateUtils.saveCertificates(options.trustStore, trustStore);
						System.out.println("Created authority certificate.");
						break;
					}
					case "list-authorities": {
						displayCertificates(trustStore);
						break;
					}
					case "create-client":
						createCertificate(trustStore, Certificate.Type.CLIENT, createClientCommand);
						break;
					case "create-server":
						createCertificate(trustStore, Certificate.Type.SERVER, createServerCommand);
						break;
					case "show-certificate": {
						final CertificateChain certificates = CertificateUtils.loadCertificateChain(showCertificateCommand.certificate);
						displayCertificates(certificates);
						break;
					}
					case "chain-certificate":
						chainCertificate(chainCertificateCommand);
						break;
					case "validate-certificate": {
						final CertificateChain certificates = CertificateUtils.loadCertificateChain(validateCommand.certificate);
						trustStore.validateCertificateChain(certificates);
						System.out.println(validateCommand.certificate + " is valid.");
						break;
					}
				}
			}
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			commander.usage(commander.getParsedCommand());
			System.exit(1);
		}
	}

	private static void displayCertificates(Iterable<Certificate> certificates) {
		System.out.println("=============================================================");
		for (Certificate certificate : certificates) {
			System.out.println("Serial number:         " + certificate.getSerialNumber());
			System.out.println("Issuer:                " + certificate.getIssuer());
			System.out.println("Type:                  " + certificate.getType());
			final String expirationString;
			if (certificate.getExpirationDate() == -1) {
				expirationString = "Never";
			} else if (certificate.getExpirationDate() < System.currentTimeMillis()) {
				expirationString = "Expired";
			} else {
				// TODO Split this out to days, hours, minutes, seconds
				final long seconds = (certificate.getExpirationDate() - System.currentTimeMillis()) / 1000;
				expirationString = "Expires in " + formatTime(seconds);
			}
			System.out.println("Expiration:            " + expirationString);
			System.out.println("Publish permissions:   " + certificate.getPublishPermissions());
			System.out.println("Subscribe permissions: " + certificate.getSubscribePermissions());
			System.out.println("Comment: " + certificate.getComment());
			System.out.println("=============================================================");
		}
	}

	private static String formatTime(long seconds) {
		final long days = seconds / TimeUnit.DAYS.toSeconds(1);
		seconds -= TimeUnit.DAYS.toSeconds(days);
		final long hours = seconds / TimeUnit.HOURS.toSeconds(1);
		seconds -= TimeUnit.HOURS.toSeconds(hours);
		final long minutes = seconds / TimeUnit.MINUTES.toSeconds(1);
		seconds -= TimeUnit.MINUTES.toSeconds(minutes);
		return String.format("%d days, %d hours, %d minutes, %d seconds", days, hours, minutes,seconds);
	}

	private static void createCertificate(TrustStore trustStore, Certificate.Type type, AbstractCreateClientServerCommand createCommand) throws Exception {
		final Certificate issuerCertificate = trustStore.get(createCommand.issuer);
		if (issuerCertificate == null) {
			throw new IllegalArgumentException("No certificate found in trust store with serial number " + createCommand.issuer);
		}
		final PrivateKey issuerPrivateKey = CertificateUtils.loadPrivateKey(createCommand.issuerPrivateKey);
		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		CertificateUtils.savePrivateKey(keyPair.getPrivate(), createCommand.privateKey);
		final Certificate certificate = CertificateUtils.generateSignedCertificate(
				issuerCertificate,
				issuerPrivateKey,
				keyPair.getPublic(),
				type,
				getExpirationDate(createCommand.expirationDate),
				Subject.list(createCommand.subscribePermissions),
				Subject.list(createCommand.publishPermissions),
				createCommand.comment
		);
		final CertificateChain chain = new CertificateChain(certificate);
		CertificateUtils.saveCertificates(createCommand.certificate, chain);
	}

	private static void chainCertificate(ChainCertificateCommand command) throws Exception {
		final CertificateChain certificates = CertificateUtils.loadCertificateChain(command.existingCertificate);
		final PrivateKey privateKey = CertificateUtils.loadPrivateKey(command.existingPrivateKey);

		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final Certificate certificate = CertificateUtils.generateSignedCertificate(
				certificates.getLast(),
				privateKey,
				keyPair.getPublic(),
				certificates.getLast().getType(),
				getExpirationDate(command.expirationDate),
				Subject.list(command.subscribePermissions),
				Subject.list(command.publishPermissions),
				command.comment
		);
		certificates.add(certificate);
		CertificateUtils.saveCertificates(command.certificate, certificates);
		CertificateUtils.savePrivateKey(keyPair.getPrivate(), command.privateKey);
	}

	private static long getExpirationDate(long expirationDate) {
		return expirationDate == -1 ? -1 : System.currentTimeMillis() + expirationDate * 1000;
	}

	private static class AbstractCreateCommand {
		@Parameter(names = "-privateKey", description = "The file to store the private key for the certificate being created.", required = true)
		String privateKey;

		@Parameter(names = "-expirationDate", description = "The time, in seconds, until the certifcate expires. Use -1 for no expiration.")
		long expirationDate = -1;

		@Parameter(names = "-comment", description = "A comment to associate with the certificate.")
		String comment;

		@Parameter(names = "-publishPermission", description = "Specifies a subject on which the certificate is authorized to publish.")
		List<String> publishPermissions = Arrays.asList("*");

		@Parameter(names = "-subscribePermission", description = "Specifies a subject on which the certificate is authorized to subscribe.")
		List<String> subscribePermissions = Arrays.asList("*");
	}

	@Parameters(commandNames = "create-authority", commandDescription = "Creates an authority certificate and adds it to the trust store")
	private static class CreateAuthorityCommand extends AbstractCreateCommand {

	}

	private static abstract class AbstractCreateClientServerCommand extends AbstractCreateCommand {

		@Parameter(names="-issuer", description = "The id of the issuer certificates in the trust store", required = true)
		long issuer;

		@Parameter(names = "-issuerPrivateKey", description = "The file storing the issuer certificate's private key.", required = true)
		String issuerPrivateKey;

		@Parameter(names = "-certificate", description = "The file in which to store the certificate", required = true)
		String certificate;
	}

	@Parameters(commandNames = "create-client", commandDescription = "Creates a client certificate")
	private static class CreateClientCommand extends AbstractCreateClientServerCommand {
	}

	@Parameters(commandNames = "create-server", commandDescription = "Creates a server certificate")
	private static class CreateServerCommand extends AbstractCreateClientServerCommand {
	}

	@Parameters(commandNames = "chain-certificate", commandDescription = "Chains a new certificate to an existing certificate or certificate chain")
	private static class ChainCertificateCommand extends AbstractCreateCommand {

		@Parameter(names = "-existingPrivateKey", description = "The private key of the certificate being chained or the private key of the last certificate in the chain", required = true)
		String existingPrivateKey;

		@Parameter(names = "-existingCertificate", description = "The file name of the existing certificate or certificate chain", required = true)
		String existingCertificate;

		@Parameter(names = "-certificate", description = "The file in which to store the new certificate chain", required = true)
		String certificate;
	}

	@Parameters(commandNames = "list-authorities", commandDescription = "List the certificates in the trust store.")
	private static class ListAuthorities {
	}

	@Parameters(commandNames = "show-certificate", commandDescription = "Displays the contents of a certificate.")
	private static class ShowCertificateCommand {
		@Parameter(names = "-certificate", description = "The certificate or certificate chain to display", required = true)
		String certificate;
	}

	@Parameters(commandNames = "validate-certificate", commandDescription = "Validates a certificate or certificate chain against the trust store")
	private static class ValidateCommand {
		@Parameter(names = "-certificate", description = "The certificate or certificate chain to validate", required = true)
		String certificate;
	}

}
