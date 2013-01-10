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

import cloudeventbus.pki.CertificateChain;

/**
 * Holds the response to an authentication request.
 *
 * @author Mike Heath <elcapo@gmail.com>
 */
public class AuthenticationResponseFrame implements Frame {

	/**
	 * The certificates that identify the client.
	 */
	private final CertificateChain certificates;

	/**
	 * Random bytes sent by the client as part of the digital signature.
	 */
	private final byte[] salt;

	/**
	 * The digital signature. This consists of the challenge sent by the server in the
	 * {@link AuthenticationRequestFrame} concatenated with the {@code salt} and encrypted with the private key
	 * associated with the the last certificate in the certificate chain.
	 */
	private final byte[] digitalSignature;

	public AuthenticationResponseFrame(CertificateChain certificates, byte[] salt, byte[] digitalSignature) {
		this.certificates = certificates;
		this.salt = salt.clone();
		this.digitalSignature = digitalSignature;
	}

	public CertificateChain getCertificates() {
		return certificates;
	}

	public byte[] getSalt() {
		return salt.clone();
	}

	public byte[] getDigitalSignature() {
		return digitalSignature.clone();
	}

	@Override
	public String toString() {
		return "Authentication response";
	}

	@Override
	public FrameType getFrameType() {
		return FrameType.AUTH_RESPONSE;
	}
}
