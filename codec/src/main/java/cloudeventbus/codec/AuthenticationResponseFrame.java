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

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AuthenticationResponseFrame implements Frame {

	/**
	 * The X.509 certificate that identifies the client (Base64 encoded.)
	 */
	private final String certificate;

	/**
	 * Random bytes sent by the client as part of the digital signature (Base64 encoded).
	 */
	private final String salt;

	/**
	 * The digital signature. This consists of the challenge sent by the server in the
	 * {@link AuthenticationRequestFrame} concatenated with the {@code salt} and encrypted with the private key associated
	 * with the public key stored in the {@code certificate}.
	 */
	private final String digitalSignature;

	public AuthenticationResponseFrame(String certificate, String salt, String digitalSignature) {
		this.certificate = certificate;
		this.salt = salt;
		this.digitalSignature = digitalSignature;
	}

	public String getCertificate() {
		return certificate;
	}

	public String getSalt() {
		return salt;
	}

	public String getDigitalSignature() {
		return digitalSignature;
	}
}
