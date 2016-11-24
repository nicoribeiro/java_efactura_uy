package com.bluedot.efactura.commons;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

public class EfacturaSecurity
{

	public static byte[] getCertificate(String certName, String certificatePassword, KeyStore keystore) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException,
			CertificateEncodingException
	{
		/*
		 * Load the KeyStore and get the signing key and certificate.
		 */
		KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(certName, new KeyStore.PasswordProtection(certificatePassword.toCharArray()));

		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

		return cert.getEncoded();
	}

}
