/*
 *   Copyright (c) 2012, 2013 Mike Heath.  All rights reserved.
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
 * The root exception for all Cloud Event Bus exceptions.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CloudEventBusException extends RuntimeException {

	/**
	 * Creates a new exception with the specified message.
	 * @param message a description of the error condition that produced this exception
	 */
	public CloudEventBusException(String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the specified message.
	 * @param message a description of the error condition that produced this exception.
	 * @param cause the root cause of the exception
	 */
	public CloudEventBusException(String message, Throwable cause) {
		super(message, cause);
	}
}
