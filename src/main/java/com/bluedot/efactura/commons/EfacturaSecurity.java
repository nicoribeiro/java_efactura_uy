package com.bluedot.efactura.commons;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.bluedot.commons.IO;
import com.bluedot.commons.PrettyPrint;
import com.bluedot.commons.XmlSignature;
import com.bluedot.efactura.EFacturaException;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;

public class EfacturaSecurity
{
//TODO no se usa este metodo
	public static EnvioCFE addInternalSignature(EnvioCFE envioCFE) throws EFacturaException
	{

		try
		{
			CFEDefType cfe = envioCFE.getCFE().get(0);

			String filenamePrefix = Commons.getFilenamePrefix(cfe);

			StringWriter unsignedWriter = new StringWriter();
			StringWriter signedWriter = new StringWriter();

			// Create the JAXBContext
			JAXBContext context = JAXBContext.newInstance(EnvioCFE.class);

			// Create the marshaller
			Marshaller marshaller = context.createMarshaller();
			//marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());

			// Marshal the Object to a file
			marshaller.marshal(envioCFE, unsignedWriter);

			// Dump to file
			IO.writeFile(filenamePrefix + "_unsigned.xml", PrettyPrint.prettyPrintXML(unsignedWriter.toString()));

			// Create the Document
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document document = db.newDocument();

			// Marshall the Object to a Document
			marshaller.marshal(envioCFE, document);

			KeyStore keystore = Commons.getKeyStore();

			String certName = Commons.getCetificateAlias();

			String certPass = Commons.getCertificatePassword();

			XmlSignature xmlSignature = new XmlSignature(certName, certPass, keystore);
			xmlSignature.sign(document);

			// Create the Unmarshaller
			Unmarshaller unmarshaller = context.createUnmarshaller();

			// Unmarshall the object
			envioCFE = (EnvioCFE) unmarshaller.unmarshal(document);

			// Marshall the object to file
			marshaller.marshal(envioCFE, signedWriter);

			// Dump to file
			IO.writeFile(filenamePrefix + "_signed.xml", PrettyPrint.prettyPrintXML(signedWriter.toString()));

			return envioCFE;

		} catch (JAXBException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvalidAlgorithmParameterException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableEntryException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MarshalException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLSignatureException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;

	}

	

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
