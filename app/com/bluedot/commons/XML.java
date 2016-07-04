package com.bluedot.commons;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import javax.xml.bind.JAXBContext;
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

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.EnvioCFE;

public class XML {

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
		File file = new File(filepath);
		return XML.readDocument(file);
	}

	public static Document readDocument(File file) throws SAXException, IOException, ParserConfigurationException {
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
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
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();

		return builder.parse(new ByteArrayInputStream(xml.getBytes()));
	}

	public static Object unMarshall(Document document, Class classToHandle) throws JAXBException {

		JAXBContext jaxbContext = JAXBContext.newInstance(classToHandle);

		Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
		return jaxbUnmarshaller.unmarshal(document);

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
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document document = db.newDocument();

		// Marshall the Object to a Document
		marshaller.marshal(object, document);

		return document;

	}
}
