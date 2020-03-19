package com.bluedot.efactura.interceptors;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.XML;
import com.bluedot.commons.utils.XmlSignature;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;
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
				
				
				if (list.get(0) instanceof WSEFacturaEFACRECEPCIONSOBRE){
					/*
					 * Es un sobre con CFEs dentro
					 */
					SobreEmitido sobre = InterceptorContextHolder.getSobreEmitido();
					if (sobre.isReenvio())
						return;
					signSobre(sobre, message);
				}
				
				if (list.get(0) instanceof WSEFacturaEFACRECEPCIONREPORTE){
					/*
					 * Es un reporte diario
					 */
					Empresa empresa = InterceptorContextHolder.getEmpresa();
					ReporteDiario reporteDiario = InterceptorContextHolder.getReporteDiario();
					signReporte(message, empresa, reporteDiario);
				}
				
				
			} catch (TransformerFactoryConfigurationError | Exception | APIException  e) {
				e.printStackTrace();
				throw new Fault(e);
			}
		

	}

	private void signReporte(Message message, Empresa empresa, ReporteDiario reporteDiario) throws TransformerFactoryConfigurationError, APIException, Exception{
		List list = message.getContent(java.util.List.class);
		
		WSEFacturaEFACRECEPCIONREPORTE sobre = (WSEFacturaEFACRECEPCIONREPORTE) list.get(0);
		
		Data data = sobre.getDatain();
		
		/*
		 * Instantiate the report
		 */
		Document allDocument= XML.loadXMLFromString(data.getXmlData());
		
		/*
		 * Isolate the Reporte 
		 */
		Document cfeDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Node unsignedNode = allDocument.getElementsByTagName("Reporte").item(0);
		cfeDocument.adoptNode(unsignedNode);
		cfeDocument.appendChild(unsignedNode);
		
		/*
		 * Key Store
		 */
		KeyStore keystore = empresa.getFirmaDigital().getKeyStore();

		
		/*
		 * Firmar el Reporte
		 */
		XmlSignature xmlSignature = new XmlSignature(FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD, keystore);
		xmlSignature.sign(cfeDocument);
		
		
		
		/*
		 *  Build the message again 
		 */
		Node signedNode = cfeDocument.getDocumentElement();
		unsignedNode = allDocument.getElementsByTagName("Reporte").item(0);
		String documentString = XML.documentToString(signedNode);
		data.setXmlData(documentString);
		message.setContent(List.class, list);
		
		reporteDiario.setXml(documentString);

		
	}

	private void signSobre(SobreEmitido sobreEmitido, Message message) throws TransformerFactoryConfigurationError, APIException, Exception {
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
		
		allDocument = signDocument(dbf, allDocument,"ns0:CFE","DGICFE:EnvioCFE", sobreEmitido.getEmpresaEmisora().getFirmaDigital().getKeyStore(), FirmaDigital.KEY_ALIAS, FirmaDigital.KEYSTORE_PASSWORD);
		
		String documentString = XML.documentToString(allDocument);
		data.setXmlData(documentString);
		message.setContent(List.class, list);
		
		sobreEmitido.setXmlDgi(documentString);
		
		/*
		 * Extraigo el hash del CFE
		 */
		EnvioCFE envioCFE = (EnvioCFE) XML.unMarshall(XML.loadXMLFromString(sobre.getDatain().getXmlData()), EnvioCFE.class);
		
		for (Iterator<CFEDefType> iterator = envioCFE.getCVES().iterator(); iterator.hasNext();) {
			CFEDefType cfeDefType = iterator.next();
			
			if (cfeDefType.getEFact()!=null){
				IdDocFact id = cfeDefType.getEFact().getEncabezado().getIdDoc();
				CFE cfe = sobreEmitido.getCFE(id.getNro().intValue(), id.getSerie().toString(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
				cfe.setHash(cfeDefType.getSignature().getSignedInfo().getReferences().get(0).getDigestValue());
			}
			
			if (cfeDefType.getETck()!=null){
				IdDocTck id = cfeDefType.getETck().getEncabezado().getIdDoc();
				CFE cfe = sobreEmitido.getCFE(id.getNro().intValue(), id.getSerie().toString(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
				cfe.setHash(cfeDefType.getSignature().getSignedInfo().getReferences().get(0).getDigestValue());
			}
			
			if (cfeDefType.getEResg()!=null){
				IdDocResg id = cfeDefType.getEResg().getEncabezado().getIdDoc();
				CFE cfe = sobreEmitido.getCFE(id.getNro().intValue(), id.getSerie().toString(), TipoDoc.fromInt(id.getTipoCFE().intValue()));
				cfe.setHash(cfeDefType.getSignature().getSignedInfo().getReferences().get(0).getDigestValue());
			}
			
			//TODO completar los documentos que faltan
			
		}
		
	}

	public static Document signDocument(DocumentBuilderFactory dbf, Document allDocument, String childTagName, String parentTagName, KeyStore keystore, String certName, String certPass)
			throws ParserConfigurationException, FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException, Exception {
		/*
		 * Isolate the CFE 
		 */
		Document cfeDocument = dbf.newDocumentBuilder().newDocument();
		
		if (childTagName!=null){
			Node unsignedNode = allDocument.getElementsByTagName(childTagName).item(0);
			cfeDocument.adoptNode(unsignedNode);
			cfeDocument.appendChild(unsignedNode);
		}
		
		/*
		 * Sign the CFE
		 */
		XmlSignature xmlSignature = new XmlSignature(certName, certPass, keystore);
		if (childTagName==null)
			xmlSignature.sign(allDocument);
		else
			xmlSignature.sign(cfeDocument);
		
		
		/*
		 *  Build the message again 
		 */
		
		if (parentTagName!=null){
			Node signedNode = allDocument.importNode(cfeDocument.getElementsByTagName(childTagName).item(0), true);
			allDocument.getElementsByTagName(parentTagName).item(0).appendChild(signedNode);
		}else
			if (childTagName!=null)
				allDocument = cfeDocument;
		
		return allDocument;
		

	}
	
}
