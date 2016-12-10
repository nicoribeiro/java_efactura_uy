package com.bluedot.efactura.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.PrettyPrint;
import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.model.TipoDoc;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.reporte.ReporteDefType;
import dgi.soap.recepcion.Data;
import play.Application;
import play.Environment;

@Singleton
public class Commons {
	private String securityPrefixName = "org.apache.ws.security.crypto.merlin.keystore.";
	private SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
	private Properties securityProperties;
	
	private Provider<Application> application;
	
	private Environment environment;
	
	@Inject
	public Commons(Provider<Application> application, Environment environment){
		this.application = application;
		this.environment = environment;
	}
	
	public enum DgiService {
		Recepcion, Consulta, Rut
	}

	public String getURL(DgiService service) {

		String environment = application.get().configuration().getString(Constants.ENVIRONMENT);

		switch (service) {
		case Consulta:
			return application.get().configuration().getString(Constants.SERVICE_CONSULTA_PREFIX + "." + environment);
		case Recepcion:
			return application.get().configuration().getString(Constants.SERVICE_RECEPCION_URL_PREFIX + "." + environment);
		case Rut:
			return application.get().configuration().getString(Constants.SERVICE_RUT_URL_PREFIX + "." + environment);
		}
		return null;

	}

	public KeyPasswordCallback getPasswordCallback() throws FileNotFoundException, IOException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException {

		Map<String, String> keystorePasswords = new HashMap<>();
		keystorePasswords.put(getCetificateAlias(), getCertificatePassword());
		return new KeyPasswordCallback(keystorePasswords);

	}

	public KeyStore getKeyStore() throws IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		KeyStore keystore = KeyStore.getInstance(getSecurityProperties().getProperty(securityPrefixName + "type"));
		InputStream fIn = Commons.class.getClassLoader().getResourceAsStream(getSecurityProperties().getProperty(securityPrefixName + "file"));
		keystore.load(fIn, getSecurityProperties().getProperty(securityPrefixName + "password").toCharArray());

		return keystore;
	}

	public String getCertificatePassword() throws FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		return getSecurityProperties().getProperty("certificate.password");
	}

	public String getCetificateAlias() throws FileNotFoundException, IOException {
		return getSecurityProperties().getProperty("certificate.alias");
	}

	public JSONObject safeGetJSONObject(JSONObject object, String key) throws APIException {
		if (object.optJSONObject(key) == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams(key));
		return object.getJSONObject(key);
	}
	
	public JSONArray safeGetJSONArray(JSONObject object, String key) throws APIException {
		if (object.optJSONArray(key) == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams(key));
		return object.getJSONArray(key);
	}
	
	public String safeGetString(JSONObject object, String key) throws APIException {
		if (object.optString(key).equals(""))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams(key));
		return object.optString(key);
	}
	
	public Integer safeGetInteger(JSONObject object, String key) throws APIException {
		if (!object.has(key))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams(key));
		return object.optInt(key);
	}

	public String getFilenamePrefix(CFEDefType cfe)
			throws APIException, FileNotFoundException, IOException {

		String type = null;
		Date date = null;
		String serie = null;
		String nro = null;

		if (cfe.getEFact() != null) {
			date = cfe.getEFact().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getEFact().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getEFact().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getEFact().getEncabezado().getIdDoc().getTipoCFE().toString();

		}

		if (cfe.getERem() != null) {
			date = cfe.getERem().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getERem().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getERem().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getERem().getEncabezado().getIdDoc().getTipoCFE().toString();
		}

		if (cfe.getETck() != null) {
			date = cfe.getETck().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getETck().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getETck().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getETck().getEncabezado().getIdDoc().getTipoCFE().toString();
		}

		if (cfe.getEFactExp() != null) {
			date = cfe.getEFactExp().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getEFactExp().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getEFactExp().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getEFactExp().getEncabezado().getIdDoc().getTipoCFE().toString();
		}
		if (cfe.getERemExp() != null) {
			date = cfe.getERemExp().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getERemExp().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getERemExp().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getERemExp().getEncabezado().getIdDoc().getTipoCFE().toString();
		}

		if (cfe.getEResg() != null) {
			date = cfe.getEResg().getTmstFirma().toGregorianCalendar().getTime();
			serie = cfe.getEResg().getEncabezado().getIdDoc().getSerie();
			nro = cfe.getEResg().getEncabezado().getIdDoc().getNro().toString();
			type = cfe.getEResg().getEncabezado().getIdDoc().getTipoCFE().toString();
		}

		if (type == null)
			throw APIException.raise(APIErrors.MALFORMED_CFE)
					.setDetailMessage("No se encontro un cfe dentro del CFEDefType");

		return getCfeFolder(date, TipoDoc.fromInt(Integer.parseInt(type))) + File.separator + serie + "_" + nro;

	}

	public String getCfeFolder(Date date, TipoDoc tipo) throws FileNotFoundException, IOException {
		return getCfeFolder(date) + File.separator + tipo.value;

	}

	public String getCfeFolder(Date date) throws FileNotFoundException, IOException {
		String folder = application.get().configuration().getString(Constants.GENERATED_CFE_FOLDER,
				"resources" + File.separator + "cfe");
		return folder + File.separator + formatter.format(date);

	}

	public void dumpSobreToFile(EnvioCFE envioCFE, int indiceCFE, Boolean isSigned, Data response)
			throws JAXBException, FileNotFoundException, IOException, APIException, ParserConfigurationException,
			TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		Document document = XML.marshall(envioCFE);

		Node node = document.getElementsByTagName("DGICFE:CFE").item(indiceCFE);

		String filenamePrefix = this.getFilenamePrefix(envioCFE.getCVES().get(indiceCFE));

		dumpNodeToFile(node, isSigned, filenamePrefix, response);
	}

	public void dumpNodeToFile(Node node, Boolean isSigned, String filenamePrefix, Data response)
			throws JAXBException, FileNotFoundException, IOException, APIException,
			TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
//TODO reactivar el pretty print aca, ojo que se caga la prueba de homologacion con los signed
		if (isSigned)
			IO.writeFile(filenamePrefix + "_signed.xml", XML.documentToString(node));
		else
			IO.writeFile(filenamePrefix + "_unsigned.xml", PrettyPrint.prettyPrintXML(XML.documentToString(node)));

		if (response != null)
			IO.writeFile(filenamePrefix + "_response.xml", PrettyPrint.prettyPrintXML(response.getXmlData()));
	}
	
	public static void dumpEnvelopeToFile(String envelope, String filenamePrefix)
			throws JAXBException, FileNotFoundException, IOException, APIException,
			TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {

		IO.writeFile(filenamePrefix + "_envelope.xml", PrettyPrint.prettyPrintXML(envelope));
	}

	public String getFilenamePrefix(Node node) throws FileNotFoundException, IOException, ParseException {
		SimpleDateFormat reader = new SimpleDateFormat("yyyy-MM-dd");
		String folder = application.get().configuration().getString(Constants.GENERATED_CFE_FOLDER,
				"resources" + File.separator + "cfe");
		Node encabezado=null;
		boolean entreEmpresas = false;
		
		if (node.getNodeName().equals("Reporte")) {
			// Es un Reporte
			Node caratula = getChildNode(node, "Caratula");
			String date = formatter.format(reader.parse(getChildValue(caratula, "FechaResumen")));

			return folder + File.separator + date + File.separator + "reporteDiario_"
					+ date + "_" + getChildValue(caratula, "SecEnvio");

		} 
		
		else if (node.getNodeName().endsWith("EnvioCFE_entreEmpresas")) { 
			// Es un CFE entre empresas
			//TODO este item(0) es porque no viene con prettyprint, con pretty print va item(1). sin va item(0) ver que no sea dependiente de esto
			encabezado = getChildNode(getChildNode(getChildNode(node,":CFE_Adenda"),":CFE").getChildNodes().item(0), ":Encabezado");
			entreEmpresas = true;
		}
		
		else if (node.getNodeName().endsWith("EnvioCFE")){
			// Es un CFE
			//TODO este item(0) es porque no viene con prettyprint, con pretty print va item(1). sin va item(0) ver que no sea dependiente de esto
			encabezado = getChildNode(getChildNode(node,":CFE").getChildNodes().item(0), ":Encabezado");
		}
		
		if (encabezado!=null){
			Node idDoc = getChildNode(encabezado, ":IdDoc");
	
			String type = getChildValue(idDoc, ":TipoCFE");
			String date = formatter.format(reader.parse(getChildValue(idDoc, ":FchEmis")));
			String serie = getChildValue(idDoc, ":Serie");
			String nro = getChildValue(idDoc, ":Nro");
	
			return folder + File.separator + date + File.separator + type + File.separator + serie + "_" + nro + (entreEmpresas?"_entre_empresas":"");
		}
		return null;
	}

	private static String getChildValue(Node node, String name) {
		Node result = getChildNode(node, name);
		return result.getTextContent();
	}

	private static Node getChildNode(Node node, String name) {
		for (int i = 0; i < node.getChildNodes().getLength(); i++) {
			String nodeName = node.getChildNodes().item(i).getNodeName();

			if (nodeName.endsWith(name))
				return node.getChildNodes().item(i);
		}
		return null;
	}

	public void dumpReporteToFile(ReporteDefType reporte, Boolean isSigned, Data response, Date date)
			throws JAXBException, TransformerConfigurationException, IOException, TransformerFactoryConfigurationError,
			TransformerException, ParserConfigurationException, APIException, ParseException {
		// Create the JAXBContext
		JAXBContext context = JAXBContext.newInstance(ReporteDefType.class);
		// Create the marshaller
		Marshaller marshaller = context.createMarshaller();
		// Create the Document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();

		// Marshall the Object to a Document
		marshaller.marshal(reporte, document);

		Node node = document.getDocumentElement();

		dumpNodeToFile(node, isSigned, getFilenamePrefix(node), response);

	}

	public String getRucDGI() {
		
		String environment = application.get().configuration().getString(Constants.ENVIRONMENT);

		return application.get().configuration().getString(Constants.RUC_DGI + "." + environment, "219999830019");
		
	}

	public Properties getSecurityProperties() {
		if (securityProperties==null){
		securityProperties = new Properties();
		try {
			if (environment.isDev())
				securityProperties.load(new FileInputStream(application.get().configuration().getString(Constants.SECURITY_FILE)));
			else
				securityProperties.load(Commons.class.getClassLoader().getResourceAsStream(application.get().configuration().getString(Constants.SECURITY_FILE)));
		} catch (IOException e) {
			e.printStackTrace();
		}
		}
		return securityProperties;
	}

}
