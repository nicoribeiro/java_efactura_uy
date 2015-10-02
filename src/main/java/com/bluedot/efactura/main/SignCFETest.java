package com.bluedot.efactura.main;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.xml.sax.SAXException;

import com.bluedot.commons.XmlSignature;
import com.bluedot.efactura.commons.Commons;

public class SignCFETest
{

	private static String inputFilename = "resources/examples/cfe-unsigned.xml";
	private static String outputFilename = "resources/examples/cfe-signedByApp.xml";

	
	
	public static void main(String[] args)
	{
		try
		{
			KeyStore keystore = Commons.getKeyStore();
			
			String certName = Commons.getCetificateAlias();
			
			String certPass = Commons.getCertificatePassword();
			
			XmlSignature.sign(certName, certPass, keystore, inputFilename, outputFilename, "ns0:CFE");
			
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException | UnrecoverableEntryException | IOException | SAXException
				| ParserConfigurationException | MarshalException | XMLSignatureException | TransformerFactoryConfigurationError | TransformerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
