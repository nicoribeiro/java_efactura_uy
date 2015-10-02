package com.bluedot.commons;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class XmlSignature
{

	public static void sign(String certName, String certificatePassword, KeyStore keystore, String inputFilename, String outputFilename, String nodeName) throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, SAXException, IOException, ParserConfigurationException, FileNotFoundException, MarshalException,
			XMLSignatureException, TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException
	{
		/*
		 * Instantiate the document to be signed.
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(inputFilename));
		
		doc = sign(certName, certificatePassword,keystore, doc, nodeName);
		
		/*
		 * Output the resulting document.
		 */
		OutputStream os = new FileOutputStream(outputFilename);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(os));
		
	}

	public static Document sign(String certName, String certificatePassword, KeyStore keystore, Document docToSign, String nodeName) throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, UnrecoverableEntryException, KeyStoreException, SAXException, IOException, ParserConfigurationException, FileNotFoundException, MarshalException,
			XMLSignatureException, TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException
	{
		/*
		 * Create a DOM XMLSignatureFactory that will be used to generate the
		 * enveloped signature.
		 */
		XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

		/*
		 * Create a Reference to the enveloped document (in this case, you are
		 * signing the whole document, so a URI of "" signifies that, and also
		 * specify the SHA1 digest algorithm and the ENVELOPED Transform.
		 */
		Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null), Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null, null);

		/*
		 * Create the SignedInfo.
		 */
		SignedInfo si = fac.newSignedInfo(fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null), fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null),
				Collections.singletonList(ref));

		/*
		 * Load the KeyStore and get the signing key and certificate.
		 */
		KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(certName, new KeyStore.PasswordProtection(certificatePassword.toCharArray()));

		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

		/*
		 * Create the KeyInfo containing the X509Data.
		 */
		KeyInfoFactory kif = fac.getKeyInfoFactory();
		List x509Content = new ArrayList();
		x509Content.add(cert.getSubjectX500Principal().getName());
		x509Content.add(cert);
		X509Data xd = kif.newX509Data(x509Content);
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));


		/*
		 * Create a DOMSignContext and specify the RSA PrivateKey and location
		 * of the resulting XMLSignature's parent element.
		 */
		DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), docToSign.getElementsByTagName(nodeName).item(0));

		/*
		 * Create the XMLSignature, but don't sign it yet.
		 */
		XMLSignature signature = fac.newXMLSignature(si, ki);

		/*
		 * Marshal, generate, and sign the enveloped signature.
		 */
		signature.sign(dsc);
		
		return docToSign;

		
	}
	
	

}
