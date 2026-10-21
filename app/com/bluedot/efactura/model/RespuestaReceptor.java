package com.bluedot.efactura.model;

import java.util.LinkedList;
import java.util.List;

import com.bluedot.commons.utils.XML;

/**
 * Respuesta a CFE (ACKCFE, ME_) de la empresa receptora de un sobre emitido: el estado de cada CFE (AE aceptado, BE
 * rechazado). Se lee del payload guardado, no hay un campo por CFE.
 */
public class RespuestaReceptor {

	/** Estado de un CFE que viene en la respuesta pero sin estado legible, o que no esta en la respuesta */
	public static final String RECIBIDA = "RECIBIDA";

	private static class Det {
		long tipo;
		String serie;
		long nro;
		String estado;
	}

	private final List<Det> dets = new LinkedList<Det>();

	private RespuestaReceptor() {
	}

	/** Lee el payload de la respuesta; si no se puede leer queda sin detalles (todos RECIBIDA) */
	public static RespuestaReceptor leer(String payload) {
		RespuestaReceptor r = new RespuestaReceptor();
		if (payload == null || payload.isEmpty())
			return r;
		try {
			javax.xml.parsers.DocumentBuilderFactory dbf = XML.documentBuilderFactory();
			dbf.setNamespaceAware(true);
			javax.xml.parsers.DocumentBuilder builder = dbf.newDocumentBuilder();
			// Sin handler el parser escribe "[Fatal Error] ..." en la consola
			builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
				@Override
				public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
					throw e;
				}
			});
			String texto = payload.charAt(0) == '﻿' ? payload.substring(1) : payload;
			org.w3c.dom.NodeList nodos = builder.parse(new org.xml.sax.InputSource(new java.io.StringReader(texto)))
					.getElementsByTagNameNS("http://cfe.dgi.gub.uy", "ACKCFE_Det");
			for (int i = 0; i < nodos.getLength(); i++) {
				org.w3c.dom.Element nodo = (org.w3c.dom.Element) nodos.item(i);
				Det det = new Det();
				// Tipo y numero como numeros: algunos receptores los mandan con ceros adelante (NroCFE 0002295)
				det.tipo = numero(hijo(nodo, "TipoCFE"));
				det.nro = numero(hijo(nodo, "NroCFE"));
				det.serie = hijo(nodo, "Serie");
				det.estado = hijo(nodo, "Estado");
				r.dets.add(det);
			}
		} catch (Exception e) {
		}
		return r;
	}

	/** Estado del CFE en la respuesta; RECIBIDA si no esta o no tiene estado */
	public String estado(TipoDoc tipo, String serie, long nro) {
		for (Det det : dets)
			if (det.tipo == tipo.value && det.nro == nro && (serie == null || serie.equals(det.serie)))
				return det.estado != null ? det.estado : RECIBIDA;
		return RECIBIDA;
	}

	private static long numero(String texto) {
		try {
			return texto != null ? Long.parseLong(texto.trim()) : -1;
		} catch (NumberFormatException e) {
			return -1;
		}
	}

	private static String hijo(org.w3c.dom.Element padre, String nombre) {
		org.w3c.dom.NodeList l = padre.getElementsByTagNameNS("http://cfe.dgi.gub.uy", nombre);
		return l.getLength() > 0 && l.item(0).getTextContent() != null ? l.item(0).getTextContent().trim() : null;
	}
}
