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

			final Connector connector = AbstractClientOptions.configureConnector(options);
			connector.autoReconnect(false);
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

	private static class Options extends AbstractClientOptions {

		@Parameter(description = "subject body")
		List<String> mainParameters;

	}

}
