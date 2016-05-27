package com.bluedot.efactura.interceptors;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.bluedot.commons.XML;
import com.bluedot.commons.XmlSignature;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.global.EFacturaException;

import dgi.soap.recepcion.Data;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONREPORTE;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBRE;

public class SignatureInterceptor extends AbstractPhaseInterceptor<Message> {

	public SignatureInterceptor() {
		super(Phase.MARSHAL);
		addAfter(NamespacesInterceptor.class.getName());
	}

	@Override
	public void handleMessage(Message message) throws Fault {
			
			try {
				/*
				 * Get the XMLData from the WSEFacturaEFACRECEPCIONSOBRE
				 */
				List list = message.getContent(java.util.List.class);
				if (list.get(0) instanceof WSEFacturaEFACRECEPCIONSOBRE)
					signSobre(message);
				
				if (list.get(0) instanceof WSEFacturaEFACRECEPCIONREPORTE)
					signReporte(message);
				
				
				
				
			} catch (TransformerFactoryConfigurationError | Exception | EFacturaException  e) {
				e.printStackTrace();
				throw new Fault(e);
			}
		

	}

	private void signReporte(Message message) throws TransformerFactoryConfigurationError, EFacturaException, Exception{
		List list = message.getContent(java.util.List.class);
		
		WSEFacturaEFACRECEPCIONREPORTE sobre = (WSEFacturaEFACRECEPCIONREPORTE) list.get(0);
		
		Data data = sobre.getDatain();
		String docString = data.getXmlData();
		
		/*
		 * Instantiate the DocumentBuilderFactory.
		 * IMPORTANT: NamespaceAwerness=true!!
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		/*
		 * Instantiate the document (Caratula + CFE)
		 */
		InputStream stream = new ByteArrayInputStream(docString.getBytes());			
		Document allDocument= dbf.newDocumentBuilder().parse(stream);
		
		/*
		 * Isolate the CFE 
		 */
		Document cfeDocument = dbf.newDocumentBuilder().newDocument();
		Node unsignedNode = allDocument.getElementsByTagName("Reporte").item(0);
		cfeDocument.adoptNode(unsignedNode);
		cfeDocument.appendChild(unsignedNode);
		
		/*
		 * Key Store & Cert 
		 */
		KeyStore keystore = Commons.getKeyStore();
		String certName = Commons.getCetificateAlias();
		String certPass = Commons.getCertificatePassword();

		
		/*
		 * Sign the CFE
		 */
		XmlSignature xmlSignature = new XmlSignature(certName, certPass, keystore);
		xmlSignature.sign(cfeDocument);
		
		
		/*
		 *  Build the message again 
		 */
		Node signedNode = cfeDocument.getDocumentElement();
		unsignedNode = allDocument.getElementsByTagName("Reporte").item(0);
		data.setXmlData(XML.documentToString(signedNode));
		message.setContent(List.class, list);

		String filenamePrefix = Commons.getFilenamePrefix(signedNode);
		
		Commons.dumpNodeToFile(signedNode, true, filenamePrefix, null);
	}

	private void signSobre(Message message) throws TransformerFactoryConfigurationError, EFacturaException, Exception {
		List list = message.getContent(java.util.List.class);
		
		WSEFacturaEFACRECEPCIONSOBRE sobre = (WSEFacturaEFACRECEPCIONSOBRE) list.get(0);
		
		Data data = sobre.getDatain();
		String docString = data.getXmlData();
		

		/*
		 * Instantiate the DocumentBuilderFactory.
		 * IMPORTANT: NamespaceAwerness=true!!
		 */
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		
		/*
		 * Instantiate the document (Caratula + CFE)
		 */
		InputStream stream = new ByteArrayInputStream(docString.getBytes());			
		Document allDocument= dbf.newDocumentBuilder().parse(stream);
		
		/*
		 * Isolate the CFE 
		 */
		Document cfeDocument = dbf.newDocumentBuilder().newDocument();
		Node unsignedNode = allDocument.getElementsByTagName("ns0:CFE").item(0);
		cfeDocument.adoptNode(unsignedNode);
		cfeDocument.appendChild(unsignedNode);
		
		/*
		 * Key Store & Cert 
		 */
		KeyStore keystore = Commons.getKeyStore();
		String certName = Commons.getCetificateAlias();
		String certPass = Commons.getCertificatePassword();

		
		/*
		 * Sign the CFE
		 */
		XmlSignature xmlSignature = new XmlSignature(certName, certPass, keystore);
		xmlSignature.sign(cfeDocument);
		
		
		/*
		 *  Build the message again 
		 */
		Node signedNode = allDocument.importNode(cfeDocument.getElementsByTagName("ns0:CFE").item(0), true);
		unsignedNode = allDocument.getElementsByTagName("ns0:CFE").item(0);
		allDocument.getElementsByTagName("DGICFE:EnvioCFE").item(0).appendChild(signedNode);
		data.setXmlData(XML.documentToString(allDocument));
		message.setContent(List.class, list);
		
		Commons.dumpNodeToFile(allDocument, true,Commons.getFilenamePrefix(signedNode), null);
		
	}
	
	
	


}
