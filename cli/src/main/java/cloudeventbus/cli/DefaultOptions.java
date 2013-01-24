package cloudeventbus.cli;

import com.beust.jcommander.Parameter;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
class DefaultOptions {

	@Parameter(names = "-trustStore", description = "Specifies the trust store to use.")
	String trustStore = System.getProperty("user.home") + "/.cloudeventbus/" + "truststore";

}
