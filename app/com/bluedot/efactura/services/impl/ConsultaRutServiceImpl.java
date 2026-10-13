package com.bluedot.efactura.services.impl;

import java.io.StringWriter;
import java.util.Objects;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.soap.SOAPFaultException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.interceptors.InterceptorContextHolder;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.pool.WSRutPool;
import com.bluedot.efactura.pool.wrappers.WSPersonaGetActEmpresarialSoapPortWrapper;
import com.bluedot.efactura.services.ConsultaRutService;

import dgi.soap.rut.WSPersonaGetActEmpresarialExecute;
import dgi.soap.rut.WSPersonaGetActEmpresarialExecuteResponse;

public class ConsultaRutServiceImpl implements ConsultaRutService {

	final static Logger logger = LoggerFactory.getLogger(ConsultaRutServiceImpl.class);

	public ConsultaRutServiceImpl() {

	}

	@Override
	public String getRutData(String rut, Empresa firmante) throws APIException {
		Objects.requireNonNull(rut, "Parameter RUT is required");
		if (firmante == null || firmante.getFirmaDigital() == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("FirmaDigital")
					.setDetailMessage("Hace falta una empresa con firma digital para firmar la consulta a DGI");

		/*
		 * La firma la hace CustomMerlin con la empresa del ThreadLocal: hay que fijarla, si no firma con la que haya quedado
		 * de otro pedido en el mismo hilo (o ninguna)
		 */
		InterceptorContextHolder.setEmpresa(firmante);

		WSPersonaGetActEmpresarialSoapPortWrapper portWrapper = null;
		try {
			portWrapper = WSRutPool.getInstance().checkOut();
			WSPersonaGetActEmpresarialExecute input = new WSPersonaGetActEmpresarialExecute();
			input.setRut(rut);
			WSPersonaGetActEmpresarialExecuteResponse output = portWrapper.getPort().execute(input);
			return output.getData();
		} catch (SOAPFaultException e) {
			String detalle = fault(e);
			logger.error("Consulta de RUT " + rut + " a DGI (firmada por " + firmante.getRut() + "): DGI respondio un error SOAP: " + detalle);
			throw errorDgi(e, detalle);
		} catch (APIException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Consulta de RUT " + rut + " a DGI (firmada por " + firmante.getRut() + ")", e);
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e);
		} finally {
			if (portWrapper != null)
				try {
					WSRutPool.getInstance().checkIn(portWrapper);
				} catch (Exception ignorado) {
				}
		}
	}

	/**
	 * El error de DGI como APIException. DGI manda el motivo en el detalle del fault (GenericFault/Respuestas/Respuesta:
	 * Codigo, Descripcion, Detalle): 5 a 7 son errores en el RUT consultado, 1 es un error de DGI y el resto (8 a 12: sin
	 * local principal, nunca tuvo actividad, cancelada, comienza a futuro) son de negocio. Sin codigo, error de
	 * comunicacion.
	 */
	private static APIException errorDgi(SOAPFaultException e, String detalleCompleto) {
		String codigo = null, detalle = null;
		try {
			if (e.getFault() != null && e.getFault().getDetail() != null) {
				org.w3c.dom.NodeList codigos = e.getFault().getDetail().getElementsByTagNameNS("*", "Codigo");
				org.w3c.dom.NodeList detalles = e.getFault().getDetail().getElementsByTagNameNS("*", "Detalle");
				if (codigos.getLength() > 0)
					codigo = codigos.item(0).getTextContent().trim();
				if (detalles.getLength() > 0)
					detalle = utf8(detalles.item(0).getTextContent().trim());
			}
		} catch (Exception ignorado) {
		}
		if (codigo == null)
			return APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e).setDetailMessage("DGI respondió un error: " + detalleCompleto);
		String mensaje = "DGI código " + codigo + (detalle != null ? ": " + detalle : "");
		switch (codigo) {
		case "5":
		case "6":
		case "7":
			return APIException.raise(APIErrors.BAD_PARAMETER_VALUE, e).withParams("rut").setDetailMessage(mensaje);
		case "1":
			return APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e).setDetailMessage(mensaje);
		default:
			return APIException.raise(APIErrors.RUT_DGI, e).setDetailMessage(mensaje);
		}
	}

	/**
	 * DGI manda el detalle del error en UTF-8 pero se decodifica como ISO-8859-1 ("PaÃ\u00ads" en vez de "País"): si el texto
	 * tiene esa marca se vuelve a decodificar. Si no, queda como vino.
	 */
	static String utf8(String texto) {
		if (texto == null || texto.indexOf('\u00c3') < 0)
			return texto;
		String corregido = new String(texto.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1), java.nio.charset.StandardCharsets.UTF_8);
		return corregido.indexOf('\ufffd') < 0 ? corregido : texto;
	}

	/** Codigo, texto y el XML completo del fault (DGI a veces no manda el texto) */
	private static String fault(SOAPFaultException e) {
		StringBuilder sb = new StringBuilder();
		try {
			if (e.getFault() != null) {
				sb.append("code=").append(e.getFault().getFaultCode()).append(" string=").append(e.getFault().getFaultString());
				StringWriter xml = new StringWriter();
				javax.xml.transform.Transformer t = TransformerFactory.newInstance().newTransformer();
				t.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
				t.transform(new DOMSource(e.getFault()), new StreamResult(xml));
				sb.append(" xml=").append(xml);
			}
		} catch (Exception ex) {
			sb.append(" (no se pudo leer el fault: ").append(ex.getMessage()).append(")");
		}
		return sb.length() > 0 ? sb.toString() : String.valueOf(e.getMessage());
	}
}
