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
package cloudeventbus;

import java.util.concurrent.TimeUnit;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Constants {

	public static final int PROTOCOL_VERSION = 1;

	/**
	 * 8k default maximum message size.
	 */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 1024 * 8;

	/**
	 * The default port for Cloud Event Bus.
	 */
	public static final int DEFAULT_PORT = 4223;

	/**
	 * By default wait 5 seconds before attempting to reconnect to a Cloud Event Bus cluster.
	 */
	public static final long DEFAULT_RECONNECT_WAIT_TIME = TimeUnit.SECONDS.toMillis(5);
	public static final String REQUEST_REPLY_SUBJECT_PREFIX = "_";
	public static final int REQUEST_REPLY_SUBJECT_SIZE = 20;

	private Constants() {
		// Don't instantiate this class.
	}

}
