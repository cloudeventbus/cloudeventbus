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
package cloudeventbus.codec;

import org.apache.commons.codec.binary.Base64;

/**
 * Represent an authentication request. The request contains a challenge consisting of a random string of bytes. The
 * client encrypts this challenge using its private key and returns the encrypted challenge to the server in an
 * {@link AuthenticationResponseFrame}.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AuthenticationRequestFrame implements Frame {

	/**
	 * Base64 encoded authentication challenge.
	 */
	private final byte[] challenge;

	public AuthenticationRequestFrame(byte[] challenge) {
		this.challenge = challenge.clone();
	}

	public byte[] getChallenge() {
		return challenge.clone();
	}

	@Override
	public String toString() {
		return "Authentication request";
	}
}
