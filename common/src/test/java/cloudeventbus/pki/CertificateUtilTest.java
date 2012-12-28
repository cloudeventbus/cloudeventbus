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
package cloudeventbus.pki;

import org.testng.annotations.Test;

import java.security.KeyPair;

/**
 * @author Mike Heath <elcapo@gmail.com>
 */
public class CertificateUtilTest {

	@Test
	public void validSignature() {
		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final byte[] challenge = CertificateUtils.generateChallenge();
		final byte[] salt = CertificateUtils.generateChallenge();

		final byte[] signature = CertificateUtils.signChallenge(keyPair.getPrivate(), challenge, salt);

		CertificateUtils.validateSignature(keyPair.getPublic(), challenge, salt, signature);
	}

	@Test(expectedExceptions = InvalidSignatureException.class)
	public void invalidSignature() {
		final KeyPair keyPair = CertificateUtils.generateKeyPair();
		final byte[] challenge = CertificateUtils.generateChallenge();
		final byte[] salt = CertificateUtils.generateChallenge();

		final byte[] signature = CertificateUtils.signChallenge(keyPair.getPrivate(), challenge, salt);

		salt[0]++;
		CertificateUtils.validateSignature(keyPair.getPublic(), challenge, salt, signature);
	}

}
