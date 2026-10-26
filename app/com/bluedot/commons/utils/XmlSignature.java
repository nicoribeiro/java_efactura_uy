package com.bluedot.commons.utils;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.keyinfo.X509IssuerSerial;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.utils.Constants;
import org.apache.xml.security.utils.ElementProxy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class XmlSignature {

	private String certName;
	private String certificatePassword;
	private KeyStore keystore;

	public XmlSignature() {

	}

	public XmlSignature(String certName, String certificatePassword, KeyStore keystore) {
		super();
		this.certName = certName;
		this.certificatePassword = certificatePassword;
		this.keystore = keystore;
	}

	public void sign(String inputFilename, String outputFilename) throws Exception {
		/*
		 * Instantiate the document to be signed.
		 */
		DocumentBuilderFactory dbf = XML.documentBuilderFactory();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(new FileInputStream(inputFilename));

		doc = sign(doc);

		/*
		 * Output the resulting document.
		 */
		OutputStream os = new FileOutputStream(outputFilename);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(os));

	}

	public void sign(InputStream inputStream, OutputStream outputStream) throws Exception {
		/*
		 * Instantiate the document to be signed.
		 */
		DocumentBuilderFactory dbf = XML.documentBuilderFactory();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().parse(inputStream);

		doc = sign(doc);

		/*
		 * Output the resulting document.
		 */
		OutputStream os = outputStream;
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(doc), new StreamResult(os));

	}

	public Document sign(Document docToSign) throws Exception {
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
		Reference ref = fac.newReference("", fac.newDigestMethod(DigestMethod.SHA1, null),
				Collections.singletonList(fac.newTransform(Transform.ENVELOPED, (TransformParameterSpec) null)), null,
				null);

		/*
		 * Create the SignedInfo.
		 */
		SignedInfo si = fac.newSignedInfo(
				fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
				fac.newSignatureMethod(SignatureMethod.RSA_SHA1, null), Collections.singletonList(ref));

		/*
		 * Load the KeyStore and get the signing key and certificate.
		 */
		KeyStore.PrivateKeyEntry keyEntry = (KeyStore.PrivateKeyEntry) keystore.getEntry(certName,
				new KeyStore.PasswordProtection(certificatePassword.toCharArray()));

		X509Certificate cert = (X509Certificate) keyEntry.getCertificate();

		/*
		 * Create the KeyInfo containing the X509Data.
		 */
		KeyInfoFactory kif = fac.getKeyInfoFactory();
		List<Object> x509Content = new ArrayList<Object>();
		x509Content.add(cert.getSubjectX500Principal().getName());
		x509Content.add(cert);
		X509IssuerSerial issuer = kif.newX509IssuerSerial(cert.getIssuerX500Principal().getName(),cert.getSerialNumber());
		x509Content.add(issuer);
		X509Data xd = kif.newX509Data(x509Content);
		KeyInfo ki = kif.newKeyInfo(Collections.singletonList(xd));

		/*
		 * Create a DOMSignContext and specify the RSA PrivateKey and location
		 * of the resulting XMLSignature's parent element.
		 */
		DOMSignContext dsc = new DOMSignContext(keyEntry.getPrivateKey(), docToSign.getDocumentElement());

		// to insert Prefix to namespace of signature
		// dsc.setDefaultNamespacePrefix("firma");

		/*
		 * Create the XMLSignature, but don't sign it yet.
		 */
		XMLSignature signature = fac.newXMLSignature(si, ki);

		/*
		 * Marshal, generate, and sign the enveloped signature.
		 */
		signature.sign(dsc);

		// Find Signature element.
		NodeList nl = docToSign.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		if (nl.getLength() == 0) {
			throw new Exception("Cannot find Signature element");
		}

		// Create a DOMValidateContext and specify a KeySelector
		// and document context.
		//DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), nl.item(0));

		// Unmarshal the XMLSignature.
		//XMLSignature XMLsignature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature.
		//boolean coreValidity = XMLsignature.validate(valContext);

		//System.out.println(XML.documentToString(docToSign));
		
		return docToSign;

	}

	// otro metodo, funca igual que el anterior
	// de aca:
	// http://stackoverflow.com/questions/2052251/is-there-an-easier-way-to-sign-an-xml-document-in-java
	public ByteArrayOutputStream signFile(Document docToSign) throws Exception {
		DocumentBuilderFactory dbf = XML.documentBuilderFactory();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Node originalRoot = docToSign.getDocumentElement();
		Document copiedDocument = db.newDocument();
		Node copiedRoot = copiedDocument.importNode(originalRoot, true);
		copiedDocument.appendChild(copiedRoot);

		final Document doc = copiedDocument;
		Init.init();
		ElementProxy.setDefaultPrefix(Constants.SignatureSpecNS, "");
		// final KeyStore keyStore = loadKeyStore(privateKeyFile);
		final org.apache.xml.security.signature.XMLSignature sig = new org.apache.xml.security.signature.XMLSignature(
				doc, null, org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA);
		final Transforms transforms = new Transforms(doc);
		transforms.addTransform(Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
		sig.addDocument("", transforms, Constants.ALGO_ID_DIGEST_SHA1);
		final Key privateKey = keystore.getKey(certName, certificatePassword.toCharArray());
		final X509Certificate cert = (X509Certificate) keystore.getCertificate(certName);
		sig.addKeyInfo(cert);
		sig.addKeyInfo(cert.getPublicKey());
		sig.sign(privateKey);
		doc.getDocumentElement().appendChild(sig.getElement());
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS).canonicalizeSubtree(doc, outputStream);
		return outputStream;
	}

}
