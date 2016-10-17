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
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bluedot.commons.security.X509KeySelector;

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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
		List x509Content = new ArrayList();
		x509Content.add(cert.getSubjectX500Principal().getName());
		x509Content.add(cert);
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
		DOMValidateContext valContext = new DOMValidateContext(new X509KeySelector(), nl.item(0));

		// Unmarshal the XMLSignature.
		XMLSignature XMLsignature = fac.unmarshalXMLSignature(valContext);

		// Validate the XMLSignature.
		boolean coreValidity = XMLsignature.validate(valContext);

		System.out.println(XML.documentToString(docToSign));
		
		return docToSign;

	}

	// otro metodo, funca igual que el anterior
	// de aca:
	// http://stackoverflow.com/questions/2052251/is-there-an-easier-way-to-sign-an-xml-document-in-java
	public ByteArrayOutputStream signFile(Document docToSign) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
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
		outputStream
				.write(Canonicalizer.getInstance(Canonicalizer.ALGO_ID_C14N_WITH_COMMENTS).canonicalizeSubtree(doc));
		return outputStream;
	}

	public void veryfySignatures(String filepath) {

		// cfeXMLSignatureValidateFile: VALIDAR LAS FIRMAS DE UN SOBRE ENTERO
		// BUSCA TODOS LOS NODOS FIRMADOS Y LOS VALIDA
		String certificate64 = "";
		String contexString = "";
		String cfeString = "";
		String digest;

		try {
			javax.xml.parsers.DocumentBuilderFactory dbFactory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
			dbFactory.setNamespaceAware(true);
			javax.xml.parsers.DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			org.w3c.dom.Document doc = dBuilder
					.parse(new org.xml.sax.InputSource(new java.io.FileInputStream(filepath)));

			// Find Signatures
			org.w3c.dom.NodeList nls = doc.getElementsByTagNameNS(javax.xml.crypto.dsig.XMLSignature.XMLNS,
					"Signature");
			if (nls.getLength() == 0) {
				throw new Exception("Cannot find Signature element");
			}

			while (nls.getLength() > 0) {

				// Toma siempre primer Nodo, porque vamos "adoptando" los nodos
				// y los remueve de la lista
				org.w3c.dom.Node nodeSignature = nls.item(0);
				Node signedNode = nodeSignature.getParentNode();

				String errorMsg = "";

				// Pasa el Contexto a String (Padre del CFE para procesamiento
				// futuro)
				javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance()
						.newTransformer();
				transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
				transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
				javax.xml.transform.dom.DOMSource source = new javax.xml.transform.dom.DOMSource(
						signedNode.getParentNode());
				java.io.StringWriter sw = new java.io.StringWriter();
				transformer.transform(source, new javax.xml.transform.stream.StreamResult(sw));
				contexString = sw.toString();

				// Pasa el elemento aislado a String (para procesamiento futuro)
				source = new javax.xml.transform.dom.DOMSource(signedNode);
				sw = new java.io.StringWriter();
				transformer.transform(source, new javax.xml.transform.stream.StreamResult(sw));
				cfeString = sw.toString();

				/*
				 * Si se intenta validar sobre el documento extraido y
				 * serializado se rompe la firma, porque para darle validez al
				 * XML se pueden agregar namespaces que estaban declarados mas
				 * arriba, adulterando el documento y rompiendo la firma.
				 */

				/*
				 * Para crear el sobre existen 2 posibilidades:
				 */

				/*
				 * 1. Crear y firmar el CFE para luego incluirlo en el Sobre
				 */

				/*
				 * 2. Firmar el CFE directamente en el sobre, en este caso puede
				 * pasar que algunos namespaces estén declarados directamente en
				 * la raíz del sobre (ej. Equital). Esta situación debería estar
				 * reflejada en las Etiquetas Reference.URI y/o transforms
				 * <xpath> para indicar a que porción del documento aplica la
				 * firma. En cualquier caso, lo más adecuado es sacar el nodo a
				 * un nuevo documento para asegurarnos de que se está validando
				 * solo contra ese Nodo y no contra todo el sobre.
				 */

				// Se arma un nuevo Documento conteniendo el nodo firmado como
				// raíz.
				// Cada Nodo conserva su namespace (que podría estar declarado
				// fuera del nodo firmado),
				// pero sin alterar los atributos XML (se manejan namespaces y
				// atributos de forma independiente).
				// adoptNode tranfiere el nodo y todo su contenido (incluyendo
				// la firma) a un nuevo documento.
				// Tener en cuenta que también lo remueve de la NodeList sobre
				// la que estamos iterando.
				Document docCFE = dbFactory.newDocumentBuilder().newDocument();
				docCFE.adoptNode(signedNode);
				docCFE.appendChild(signedNode);

				try {

					// Si no tiene Certificado, lo busca dentro del nodo
					// Signature
					if (certificate64 == null || certificate64.isEmpty()) {
						NodeList nl = ((Element) nodeSignature)
								.getElementsByTagNameNS(javax.xml.crypto.dsig.XMLSignature.XMLNS, "X509Certificate");
						if (nl.getLength() > 0) {
							certificate64 = nl.item(0).getChildNodes().item(0).getNodeValue();
						} else {
							throw new Exception("No se encontró <X509Certificate> en el nodo <Signature>");
						}
					}

					// Procesa el Certificado para obtener la clave pública
					java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory
							.getInstance("X.509");
					byte[] binCert = javax.xml.bind.DatatypeConverter.parseBase64Binary(certificate64);
					java.security.cert.X509Certificate cert = (java.security.cert.X509Certificate) cf
							.generateCertificate(new java.io.ByteArrayInputStream(binCert));

					javax.xml.crypto.dsig.dom.DOMValidateContext valContext = null;
					valContext = new javax.xml.crypto.dsig.dom.DOMValidateContext(cert.getPublicKey(), nodeSignature);

					// Create a DOM XMLSignatureFactory that will be used to
					// validate the enveloped signature.
					javax.xml.crypto.dsig.XMLSignatureFactory fac = javax.xml.crypto.dsig.XMLSignatureFactory
							.getInstance("DOM");

					// Unmarshal the XMLSignature.
					javax.xml.crypto.dsig.XMLSignature signature = fac.unmarshalXMLSignature(valContext);

					// Validate the XMLSignature.
					boolean coreValidity = signature.validate(valContext);

					// Check core validation status.
					if (!coreValidity) {

						// Check the validation status of SignatureValue
						boolean sv = signature.getSignatureValue().validate(valContext);
						if (!sv) {
							errorMsg += "Error in signature validation. Certificate and signature don't match.";
						}

						// Check the validation status of each Reference.
						java.util.Iterator it = signature.getSignedInfo().getReferences().iterator();
						for (int j = 0; it.hasNext(); j++) {
							javax.xml.crypto.dsig.Reference ref = (javax.xml.crypto.dsig.Reference) it.next();
							boolean refValid = ref.validate(valContext);

							// Base64 Digest
							digest = javax.xml.bind.DatatypeConverter.printBase64Binary(ref.getDigestValue());
							String calculatedDigest = javax.xml.bind.DatatypeConverter
									.printBase64Binary(ref.getCalculatedDigestValue());

							if (!refValid) {
								errorMsg += "Error validating of reference[" + j + "]. " + digest + " <> "
										+ calculatedDigest;
							}
						}

					}

				} catch (Exception e) {
					e.printStackTrace();
					errorMsg += e.getMessage();
				}

				System.out.println(errorMsg);

				// java [!&ErrorMsgNode!] = errorMsg;
				//
				// Agrega a la colección de nodos validados
				// &cfeSignatureValidation = new()
				// &cfeSignatureValidation.context = &contexString
				// &cfeSignatureValidation.data = &cfeString
				// &cfeSignatureValidation.digest = &digest
				// &cfeSignatureValidation.error = &ErrorMsgNode
				// &validaciones.Add(&cfeSignatureValidation)
				//
			} // endwhile
			//
		} catch (Exception e) {
			// [!&ErrorMsg!] = e.getMessage();
			e.printStackTrace();
			System.err.println("Error capturado:" + e.getMessage());
		}
		// [!&Certificate64!] = certificate64;

		// &ErrorMsg = &ErrorMsg
		// &Certificate64 = &Certificate64

	}

}
