package com.bluedot.efactura.writers;

import java.util.Arrays;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
 
import org.apache.cxf.staxutils.DelegatingXMLStreamWriter;
 
/**
 * Simple CDATA XML Stream Writer that exports some items as CDATA
 */
public class CDataXMLStreamWriter extends DelegatingXMLStreamWriter {
 
	// from: http://www.codeyouneed.com/cxf-jaxrs-xml-and-cdata/
	
	// All elements with these names will be turned into CDATA
	private static String[] CDATA_ELEMENTS = { "xmlData" };
 
	private String currentElementName;
 
	public CDataXMLStreamWriter(XMLStreamWriter del) {
		super(del);
	}
 
	@Override
	public void writeCharacters(String text) throws XMLStreamException {
		if (Arrays.asList(CDATA_ELEMENTS).contains(currentElementName)) {
			super.writeCData(text);
		} else {
			super.writeCharacters(text);
		}
	}
 
	public void writeStartElement(String prefix, String local, String uri) throws XMLStreamException {
		currentElementName = local;
		super.writeStartElement(prefix, local, uri);
	}
}
