package cloudeventbus.cli;

import cloudeventbus.Subject;
import cloudeventbus.pki.Certificate;
import cloudeventbus.pki.CertificateStoreLoader;
import cloudeventbus.pki.CertificateUtils;
import cloudeventbus.pki.TrustStore;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.codec.binary.Base64OutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
// TODO Add support for creating/appending client/server certs
// TODO Add support for listing contents of client/server cert
// TODO Add support for validating certificate chain
// TODO Create scripts to launch
// TODO Write and publish docs
public class Certs {
	public static void main(String[] args) throws Exception {
		final JCommander commander = new JCommander();

		final Options options = new Options();
		final CreateAuthorityCommand createAuthorityCommand = new CreateAuthorityCommand();
		commander.addObject(options);
		commander.addCommand(createAuthorityCommand);
		commander.addCommand(new ListAuthorities());

		commander.setProgramName("ceb-certs");

		try {
			commander.parse(args);

			final String command = commander.getParsedCommand();
			if (options.help || command == null) {
				commander.usage();
			} else {
				switch(command) {
					case "create-authority": {
						final TrustStore trustStore = loadTrustStore(options.trustStore);

						final KeyPair keyPair = CertificateUtils.generateKeyPair();
						writePrivateKey(keyPair.getPrivate(), createAuthorityCommand.privateKey);
						final Certificate certificate = CertificateUtils.generateSelfSignedCertificate(
								keyPair,
								createAuthorityCommand.expirationDate == -1 ? -1 : System.currentTimeMillis() + createAuthorityCommand.expirationDate * 1000,
								Subject.list(createAuthorityCommand.subscribePermissions),
								Subject.list(createAuthorityCommand.publishPermissions),
								createAuthorityCommand.comment
						);
						trustStore.add(certificate);

						saveTrustStore(options.trustStore, trustStore);
						System.out.println("Created authority certificate.");
						break;
					}
					case "list-authorities": {
						final TrustStore trustStore = loadTrustStore(options.trustStore);
						System.out.println("=============================================================");
						for (Certificate certificate : trustStore) {
							System.out.println("Serial number:         " + certificate.getSerialNumber());
							System.out.println("Issuer:                " + certificate.getIssuer());
							final String expirationString;
							if (certificate.getExpirationDate() == -1) {
								expirationString = "Never";
							} else if (certificate.getExpirationDate() < System.currentTimeMillis()) {
								expirationString = "Expired";
							} else {
								// TODO Split this out to days, hours, minutes, seconds
								final long seconds = (certificate.getExpirationDate() - System.currentTimeMillis()) / 1000;
								expirationString = "Expires in " + seconds + " seconds";
							}
							System.out.println("Expiration:            " + expirationString);
							System.out.println("Publish permissions:   " + certificate.getPublishPermissions());
							System.out.println("Subscribe permissions: " + certificate.getSubscribePermissions());
							System.out.println("Comment: " + certificate.getComment());
							System.out.println("=============================================================");
						}
					}
				}
			}
		} catch (ParameterException e) {
			System.err.println(e.getMessage());
			commander.usage();
		}
	}

	private static class Options {
		@Parameter(names = "-trustStore", description = "Specifies the trust store to use.")
		String trustStore = "ceb-certs";

		@Parameter(names = "--help", description = "Shows this usage.", help = true, hidden = true)
		boolean help;
	}

	private static class AbstractCreateCommand {
		@Parameter(names = "-privateKey", description = "The file to store the private key.", required = true)
		String privateKey;

		@Parameter(names = "-expirationDate", description = "The time, in seconds, until the certifcate expires. User -1 for no expiration.")
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

	@Parameters(commandNames = "list-authorities", commandDescription = "List the certificates in the trust store.")
	private static class ListAuthorities {
	}

	private static void writePrivateKey(PrivateKey privateKey, String fileName) throws IOException {
		try (
				final OutputStream outputStream = Files.newOutputStream(Paths.get(fileName), StandardOpenOption.CREATE_NEW);
				final Base64OutputStream base64OutputStream = new Base64OutputStream(outputStream);
		) {
			base64OutputStream.write(privateKey.getEncoded());
		}
	}

	private static TrustStore loadTrustStore(String fileName) throws IOException {
		final Path path = Paths.get(fileName);
		final TrustStore trustStore = new TrustStore();
		if (Files.notExists(path)) {
			return trustStore;
		}
		try (
				final InputStream fileIn = Files.newInputStream(path);
		        final InputStream in = new Base64InputStream(fileIn)
		) {
			CertificateStoreLoader.load(in, trustStore);
		}
		return trustStore;
	}

	private static void saveTrustStore(String fileName, TrustStore trustStore) throws IOException {
		final Path path = Paths.get(fileName);
		try (
				final OutputStream fileOut = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		        final OutputStream out = new Base64OutputStream(fileOut)
		) {
			CertificateStoreLoader.store(out, trustStore);
		}
	}

}
