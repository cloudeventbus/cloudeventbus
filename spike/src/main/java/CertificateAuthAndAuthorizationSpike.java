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
import sun.security.util.DerValue;
import sun.security.util.ObjectIdentifier;
import sun.security.x509.AlgorithmId;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateIssuerName;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateSubjectName;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.Extension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * @author Mike Heath <heathma@ldschurch.org>
 */
public class CertificateAuthAndAuthorizationSpike {

	public static void main(String[] args) throws Exception {
		final long days = 30;
		final String dn = "cn=Mike Heath,ou=Git Hub";
		final String algorithm = "SHA1withRSA";

		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(1024);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();

		X509CertInfo info = new X509CertInfo();
		Date from = new Date();
		Date to = new Date(from.getTime() + days * 86400000l);
		CertificateValidity interval = new CertificateValidity(from, to);
		BigInteger sn = new BigInteger(64, new SecureRandom());
		X500Name owner = new X500Name(dn);

		info.set(X509CertInfo.VALIDITY, interval);
		info.set(X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber(sn));
		info.set(X509CertInfo.SUBJECT, new CertificateSubjectName(owner));
		info.set(X509CertInfo.ISSUER, new CertificateIssuerName(owner));
		info.set(X509CertInfo.KEY, new CertificateX509Key(keyPair.getPublic()));
		info.set(X509CertInfo.VERSION, new CertificateVersion(CertificateVersion.V3));
		final CertificateExtensions extensions = new CertificateExtensions();
		extensions.set("heath", new Extension(new ObjectIdentifier("1.2"), true, new DerValue(DerValue.tag_OctetString, "012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890012345678900123456789001234567890".getBytes()).toByteArray()));
		info.set(X509CertInfo.EXTENSIONS, extensions);
		  AlgorithmId algo = new AlgorithmId(AlgorithmId.md5WithRSAEncryption_oid);
		  info.set(X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId(algo));

		  // Sign the cert to identify the algorithm that's used.
		  X509CertImpl cert = new X509CertImpl(info);
		  cert.sign(keyPair.getPrivate(), algorithm);

		  // Update the algorith, and resign.
		  algo = (AlgorithmId)cert.get(X509CertImpl.SIG_ALG);
		  info.set(CertificateAlgorithmId.NAME + "." + CertificateAlgorithmId.ALGORITHM, algo);
		  cert = new X509CertImpl(info);
		  cert.sign(keyPair.getPrivate(), algorithm);

		System.out.println("cert = " + cert);

		X509Certificate plain = cert;

		final byte[] extensionValue = plain.getExtensionValue("1.2");
		final byte[] octetString = new DerValue(extensionValue).getOctetString();

		System.out.println("extensionValue = " + new String(octetString));
	}

}
