package com.bluedot.efactura.interceptors;

import java.io.OutputStream;

import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

import com.bluedot.efactura.writers.CDataXMLStreamWriter;
 
public class CDataWriterInterceptor extends AbstractPhaseInterceptor<Message> {
 
	// from: http://www.codeyouneed.com/cxf-jaxrs-xml-and-cdata/
	
	public CDataWriterInterceptor() {
		super(Phase.PRE_STREAM);
		addAfter(AttachmentOutInterceptor.class.getName());
	}
 
	@Override
	public void handleMessage(Message message) {
		// Required for CDATA to working
		message.put("disable.outputstream.optimization", Boolean.TRUE);
		XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(message.getContent(OutputStream.class));
		message.setContent(XMLStreamWriter.class, new CDataXMLStreamWriter(writer));
	}
}
