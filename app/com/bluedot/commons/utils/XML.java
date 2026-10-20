package com.bluedot.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class XML {

	/**
	 * DocumentBuilderFactory para parsear XML que puede venir de afuera (mails, DGI, la API): sin DOCTYPE, sin entidades
	 * externas, sin XInclude y con FEATURE_SECURE_PROCESSING (limites de expansion). Evita XXE (lectura de archivos y
	 * pedidos a otras maquinas) y el "billion laughs". El parser que se usa es el del classpath (Xerces 2.11), que por
	 * defecto resuelve entidades externas. Todo DocumentBuilderFactory del codigo tiene que salir de aca.
	 */
	public static DocumentBuilderFactory documentBuilderFactory() {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		try {
			dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
			dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		} catch (ParserConfigurationException e) {
			// Sin estas protecciones no se parsea nada
			throw new IllegalStateException("El parser XML (" + dbf.getClass().getName() + ") no soporta las opciones de seguridad", e);
		}
		dbf.setXIncludeAware(false);
		dbf.setExpandEntityReferences(false);
		return dbf;
	}

	public static String documentToString(Document document)
			throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(document), new StreamResult(os));

		String signed = os.toString();
		return signed;
	}

	public static String documentToString(Node node)
			throws TransformerFactoryConfigurationError, TransformerConfigurationException, TransformerException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer trans = tf.newTransformer();
		trans.transform(new DOMSource(node), new StreamResult(os));

		String signed = os.toString();
		return signed;
	}
	
	public static String objectToString(Object obj) throws JAXBException {
		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		Marshaller marshaller = context.createMarshaller();
		StringWriter sw = new StringWriter();
		marshaller.marshal(obj, sw);
		String cfe = sw.toString();
		return cfe;
	}
	
	public static Document objectToDocument(Object obj) throws JAXBException, ParserConfigurationException {
		DocumentBuilderFactory dbf = documentBuilderFactory();
		dbf.setNamespaceAware(true);
		Document doc = dbf.newDocumentBuilder().newDocument();

		JAXBContext context = JAXBContext.newInstance(obj.getClass());
		context.createMarshaller().marshal(obj, doc);

		return doc;
	}

	public static void printDocument(Document doc, OutputStream out) throws IOException, TransformerException {
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer = tf.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

		transformer.transform(new DOMSource(doc), new StreamResult(new OutputStreamWriter(out, "UTF-8")));
	}

	public static Document readDocument(String filepath)
			throws ParserConfigurationException, SAXException, IOException {
		File file = IO.resolve(filepath);
		return XML.readDocument(file);
	}

	public static Document readDocument(File file) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = documentBuilderFactory();
		dbFactory.setNamespaceAware(true);
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

		Document doc = dBuilder.parse(file);

		// optional, but recommended
		// read this -
		// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
		doc.getDocumentElement().normalize();

		return doc;
	}

	public static Document loadXMLFromString(String xml) throws Exception {
		DocumentBuilderFactory factory = documentBuilderFactory();
		/*
		 * Instantiate the DocumentBuilderFactory.
		 * IMPORTANT: NamespaceAwerness=true!!
		 */
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(xml.getBytes()));
	}

	public static Object unMarshall(Document document, Class classToHandle) throws JAXBException {

		JAXBContext jaxbContext = JAXBContext.newInstance(classToHandle);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		JAXBElement element = jaxbUnmarshaller.unmarshal(document, classToHandle);
		
		return element.getValue();

	}

	public static void marshall(Object object, OutputStream out) throws JAXBException {

		JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());

		Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

		// output pretty printed
		jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

		jaxbMarshaller.marshal(object, out);

	}

	public static Document marshall(Object object) throws JAXBException, ParserConfigurationException {

		// Create the JAXBContext
		JAXBContext context = JAXBContext.newInstance(object.getClass());
		// Create the marshaller
		Marshaller marshaller = context.createMarshaller();
		// Create the Document
		DocumentBuilderFactory dbf = documentBuilderFactory();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();

		// Marshall the Object to a Document
		marshaller.marshal(object, document);

		return document;

	}
}
