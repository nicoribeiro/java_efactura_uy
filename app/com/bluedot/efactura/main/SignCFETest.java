package com.bluedot.efactura.main;

import java.security.KeyStore;

import com.bluedot.commons.XmlSignature;
import com.bluedot.efactura.commons.Commons;

public class SignCFETest
{

	private static String inputFilename = "resources/dump/last.xml";
	private static String outputFilename = "resources/dump/lastSigned.xml";

	
	
	public static void main(String[] args)
	{
		try
		{
			KeyStore keystore = Commons.getKeyStore();
			
			String certName = Commons.getCetificateAlias();
			
			String certPass = Commons.getCertificatePassword();
			
			XmlSignature xmlSignature = new XmlSignature(certName, certPass, keystore);
			
			xmlSignature.sign(inputFilename, outputFilename);
			
		} catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
