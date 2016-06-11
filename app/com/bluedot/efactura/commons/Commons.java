package com.bluedot.efactura.commons;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.bluedot.commons.IO;
import com.bluedot.commons.PrettyPrint;
import com.bluedot.commons.Settings;
import com.bluedot.commons.XML;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.impl.KeyPasswordCallback;

import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.reporte.ReporteDefType;
import dgi.soap.recepcion.Data;

public class Commons {
	private static String securityPrefixName = "org.apache.ws.security.crypto.merlin.keystore.";
	private static SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");

	public enum DgiService {
		Recepcion, Consulta, Rut
	}

	public static String getURL(DgiService service) throws FileNotFoundException, IOException {

		Settings settings = Settings.getInstance();

		String environment = settings.getString(Constants.ENVIRONMENT);

		switch (service) {
		case Consulta:
			return settings.getString(Constants.SERVICE_CONSULTA_PREFIX + "." + environment);
		case Recepcion:
			return settings.getString(Constants.SERVICE_RECEPCION_URL_PREFIX + "." + environment);
		case Rut:
			return settings.getString(Constants.SERVICE_RUT_URL_PREFIX + "." + environment);
		}
		return null;

	}

	public static KeyPasswordCallback getPasswordCallback() throws FileNotFoundException, IOException,
			KeyStoreException, NoSuchAlgorithmException, CertificateException {

		Map<String, String> keystorePasswords = new HashMap<>();
		keystorePasswords.put(getCetificateAlias(), getCertificatePassword());
		return new KeyPasswordCallback(keystorePasswords);

	}

	public static KeyStore getKeyStore() throws FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		KeyStore keystore = KeyStore.getInstance(securityProperties.getProperty(securityPrefixName + "type"));
		FileInputStream fIn = new FileInputStream(securityProperties.getProperty(securityPrefixName + "file"));
		keystore.load(fIn, securityProperties.getProperty(securityPrefixName + "password").toCharArray());

		return keystore;
	}

	public static String getCertificatePassword() throws FileNotFoundException, IOException, KeyStoreException,
			NoSuchAlgorithmException, CertificateException {
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		return securityProperties.getProperty("certificate.password");

	}

	public static String getCetificateAlias() throws FileNotFoundException, IOException {
		Settings settings = Settings.getInstance();

		Properties securityProperties = new Properties();
		securityProperties.load(new FileInputStream(settings.getString(Constants.SECURITY_FILE)));

		return securityProperties.getProperty("certificate.alias");
	}

	public static JSONObject safeGetJSONObject(JSONObject object, String key) throws EFacturaException {
		if (object.optJSONObject(key) == null)
			throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage(key);
		return object.getJSONObject(key);
	}

	public static String getFilenamePrefix(CFEDefType cfe)
			throws EFacturaException, FileNotFoundException, IOException {

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
			throw EFacturaException.raise(EFacturaErrors.MALFORMED_CFE)
					.setDetailMessage("No se encontro un cfe dentro del CFEDefType");

		return getCfeFolder(date, TipoDoc.fromInt(Integer.parseInt(type))) + File.separator + serie + "_" + nro;

	}

	public static String getCfeFolder(Date date, TipoDoc tipo) throws FileNotFoundException, IOException {
		return getCfeFolder(date) + File.separator + tipo.value;

	}

	public static String getCfeFolder(Date date) throws FileNotFoundException, IOException {
		String folder = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
				"resources" + File.separator + "cfe");
		return folder + File.separator + formatter.format(date);

	}

	public static void dumpSobreToFile(EnvioCFE envioCFE, int indiceCFE, Boolean isSigned, Data response)
			throws JAXBException, FileNotFoundException, IOException, EFacturaException, ParserConfigurationException,
			TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {
		// Create the JAXBContext
		JAXBContext context = JAXBContext.newInstance(EnvioCFE.class);
		// Create the marshaller
		Marshaller marshaller = context.createMarshaller();
		// Create the Document
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();

		// Marshall the Object to a Document
		marshaller.marshal(envioCFE, document);

		Node node = document.getElementsByTagName("DGICFE:CFE").item(indiceCFE);

		String filenamePrefix = Commons.getFilenamePrefix(envioCFE.getCVES().get(indiceCFE));

		dumpNodeToFile(node, isSigned, filenamePrefix, response);
	}

	public static void dumpNodeToFile(Node node, Boolean isSigned, String filenamePrefix, Data response)
			throws JAXBException, FileNotFoundException, IOException, EFacturaException,
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
			throws JAXBException, FileNotFoundException, IOException, EFacturaException,
			TransformerConfigurationException, TransformerFactoryConfigurationError, TransformerException {

		IO.writeFile(filenamePrefix + "_envelope.xml", PrettyPrint.prettyPrintXML(envelope));
	}

	public static String getFilenamePrefix(Node node) throws FileNotFoundException, IOException, ParseException {
		SimpleDateFormat reader = new SimpleDateFormat("yyyy-MM-dd");
		String folder = Settings.getInstance().getString(Constants.GENERATED_CFE_FOLDER,
				"resources" + File.separator + "cfe");

		if (node.getNodeName().equals("Reporte")) {
			// Es un Reporte
			Node caratula = getChildNode(node, "Caratula");
			String date = formatter.format(reader.parse(getChildValue(caratula, "FechaResumen")));

			return folder + File.separator + date + File.separator + "reporteDiario_"
					+ date + "_" + getChildValue(caratula, "SecEnvio");

		} else {
			// Es un CFE
			Node encabezado = getChildNode(node.getChildNodes().item(1), ":Encabezado");

			Node idDoc = getChildNode(encabezado, ":IdDoc");

			String type = getChildValue(idDoc, ":TipoCFE");
			String date = formatter.format(reader.parse(getChildValue(idDoc, ":FchEmis")));
			String serie = getChildValue(idDoc, ":Serie");
			String nro = getChildValue(idDoc, ":Nro");

			return folder + File.separator + date + File.separator + type + File.separator + serie + "_" + nro;
		}
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

	public static void dumpReporteToFile(ReporteDefType reporte, Boolean isSigned, Data response, Date date)
			throws JAXBException, TransformerConfigurationException, IOException, TransformerFactoryConfigurationError,
			TransformerException, ParserConfigurationException, EFacturaException, ParseException {
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

}
