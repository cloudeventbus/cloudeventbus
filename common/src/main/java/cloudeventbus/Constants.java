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

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class Constants {

	/**
	 * 4k default maximum message size.
	 */
	public static final int DEFAULT_MAX_MESSAGE_SIZE = 4096;

	/**
	 * The default port for Cloud Event Bus.
	 */
	public static final int DEFAULT_PORT = 4223;

	private Constants() {
		// Don't instantiate this class.
	}

}
