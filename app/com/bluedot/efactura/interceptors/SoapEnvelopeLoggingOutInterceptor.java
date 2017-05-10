package com.bluedot.efactura.interceptors;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.ParseException;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.AbstractLoggingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.LoggingMessage;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.io.CacheAndWriteOutputStream;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedOutputStreamCallback;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.commons.Commons;

import dgi.soap.recepcion.Data;
import dgi.soap.recepcion.WSEFacturaEFACRECEPCIONSOBRE;

public class SoapEnvelopeLoggingOutInterceptor extends AbstractLoggingInterceptor {

	public SoapEnvelopeLoggingOutInterceptor(String phase) {
		super(phase);
		addBefore(StaxOutInterceptor.class.getName());
	}

	public SoapEnvelopeLoggingOutInterceptor() {
		this(Phase.PRE_STREAM);
	}

	public SoapEnvelopeLoggingOutInterceptor(int lim) {
		this();
		limit = lim;
	}

	public SoapEnvelopeLoggingOutInterceptor(PrintWriter w) {
		this();
		this.writer = w;
	}

	@Override
	public void handleMessage(Message message) throws Fault {
		
		
		List list = message.getContent(java.util.List.class);
		if (!(list.get(0) instanceof WSEFacturaEFACRECEPCIONSOBRE))
			return;
		
		
		final OutputStream os = message.getContent(OutputStream.class);
		final CacheAndWriteOutputStream newOut = new CacheAndWriteOutputStream(os);
		if (threshold > 0) {
			newOut.setThreshold(threshold);
		}
		if (limit > 0) {
			newOut.setCacheLimit(limit);
		}
		message.setContent(OutputStream.class, newOut);
		newOut.registerCallback(new LoggingCallback(message, os));

	}

	class LoggingCallback implements CachedOutputStreamCallback {

		private final Message message;
		private final OutputStream origStream;

		public LoggingCallback(final Message msg, final OutputStream os) {

			this.message = msg;
			this.origStream = os;
		}

		public void onFlush(CachedOutputStream cos) {

		}

		public void onClose(CachedOutputStream cos) {

			try {
				/*
				 * 
				 * 
				 * Extraccion del nodo para crear el filenamePrefix
				 * 
				 * 
				 */
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
				String filenamePrefix = Commons.getFilenamePrefix(unsignedNode);
				
				
				/*
				 * 
				 * 
				 * Seccion de dump a disco
				 * 
				 * 
				 */
				String ct = (String) message.get(Message.CONTENT_TYPE);
				StringBuilder stringBuilder = new StringBuilder();
				try {
					String encoding = (String) message.get(Message.ENCODING);
					writePayload(stringBuilder, cos, encoding, ct);
					play.Logger.info(stringBuilder.toString());
					Commons.dumpEnvelopeToFile(stringBuilder.toString(), filenamePrefix);
				} catch (Exception ex) {
					// ignore
				}

				try {
					// empty out the cache
					cos.lockOutputStream();
					cos.resetOut(null, false);
				} catch (Exception ex) {
					// ignore
				}
				message.setContent(OutputStream.class, origStream);
				
				
			} catch (SAXException | IOException | ParserConfigurationException | ParseException
					| TransformerFactoryConfigurationError | APIException e) {
				e.printStackTrace();
			}
		}
	}

	protected String formatLoggingMessage(LoggingMessage buffer) {
		return buffer.toString();
	}

	@Override
	protected Logger getLogger() {
		return LogUtils.getLogger(SoapEnvelopeLoggingOutInterceptor.class);

	}
}
