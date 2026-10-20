package com.bluedot.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Los XML que llegan por mail (sobres de otras empresas) se parsean con XML.loadXMLFromString: no se pueden aceptar
 * DOCTYPE ni entidades (XXE: lectura de archivos, pedidos a otras maquinas; "billion laughs": memoria).
 */
public class XMLTest {

	private static void rechaza(String xml) {
		try {
			XML.loadXMLFromString(xml);
		} catch (Exception e) {
			return;
		}
		fail("Se acepto: " + xml);
	}

	@Test
	public void parseaUnSobreNormal() throws Exception {
		Document doc = XML.loadXMLFromString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
				+ "<DGICFE:EnvioCFE_entreEmpresas xmlns:DGICFE=\"http://cfe.dgi.gub.uy\" version=\"1.0\">"
				+ "<DGICFE:Caratula><DGICFE:RutReceptor>219999990016</DGICFE:RutReceptor></DGICFE:Caratula>"
				+ "</DGICFE:EnvioCFE_entreEmpresas>");
		assertEquals("http://cfe.dgi.gub.uy", doc.getDocumentElement().getNamespaceURI());
		assertEquals("219999990016", doc.getElementsByTagNameNS("http://cfe.dgi.gub.uy", "RutReceptor").item(0).getTextContent());
	}

	@Test
	public void rechazaEntidadExternaDeArchivo() throws Exception {
		File secreto = File.createTempFile("xxe", ".txt");
		secreto.deleteOnExit();
		Files.write(secreto.toPath(), "SECRETO".getBytes(StandardCharsets.UTF_8));
		String xml = "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x SYSTEM \"" + secreto.toURI() + "\">]><a>&x;</a>";
		Document doc;
		try {
			doc = XML.loadXMLFromString(xml);
		} catch (Exception e) {
			return; // rechazado: bien
		}
		assertFalse("Se leyo el archivo", doc.getDocumentElement().getTextContent().contains("SECRETO"));
		fail("Se acepto un DOCTYPE");
	}

	@Test
	public void rechazaEntidadParametroExterna() {
		rechaza("<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY % p SYSTEM \"http://127.0.0.1:1/x.dtd\"> %p;]><a/>");
	}

	@Test
	public void rechazaBillionLaughs() {
		StringBuilder dtd = new StringBuilder("<!ENTITY l0 \"lol\">");
		for (int i = 1; i <= 10; i++)
			dtd.append("<!ENTITY l").append(i).append(" \"").append(new String(new char[10]).replace("\0", "&l" + (i - 1) + ";")).append("\">");
		rechaza("<?xml version=\"1.0\"?><!DOCTYPE a [" + dtd + "]><a>&l10;</a>");
	}

	@Test
	public void rechazaDoctypeAunqueNoTengaEntidades() {
		rechaza("<?xml version=\"1.0\"?><!DOCTYPE a SYSTEM \"http://127.0.0.1:1/a.dtd\"><a/>");
	}

	@Test
	public void noProcesaXInclude() throws Exception {
		File secreto = File.createTempFile("xinc", ".txt");
		secreto.deleteOnExit();
		Files.write(secreto.toPath(), "SECRETO".getBytes(StandardCharsets.UTF_8));
		Document doc = XML.loadXMLFromString("<a xmlns:xi=\"http://www.w3.org/2001/XInclude\"><xi:include href=\"" + secreto.toURI()
				+ "\" parse=\"text\"/></a>");
		assertFalse(doc.getDocumentElement().getTextContent().contains("SECRETO"));
	}

	@Test
	public void readDocumentTambienRechaza() throws Exception {
		File f = File.createTempFile("xxe", ".xml");
		f.deleteOnExit();
		Files.write(f.toPath(), "<?xml version=\"1.0\"?><!DOCTYPE a [<!ENTITY x \"y\">]><a>&x;</a>".getBytes(StandardCharsets.UTF_8));
		try {
			XML.readDocument(f);
		} catch (Exception e) {
			return;
		}
		fail("readDocument acepto un DOCTYPE");
	}
}
