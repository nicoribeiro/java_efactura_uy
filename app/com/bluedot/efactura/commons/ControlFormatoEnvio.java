package com.bluedot.efactura.commons;

import com.bluedot.commons.utils.XML;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.recibidos.ControlesRecepcion;

import play.Play;

/**
 * Control del formato del sobre (XSD de DGI EnvioCFE) antes de mandarlo a DGI, ya firmado: lo que DGI rechazaria por formato
 * (S01/S04, E05 de formato) se ve antes de gastar el numero o que rechacen el sobre (ej. CompraID de mas de 50 caracteres).
 *
 * EnvioCFE.control.formato: off (no controla), log (solo lo deja en el log y manda igual) o enforce (no manda). Si falta
 * la opcion: log. El XSD sale de EnvioCFE.formato.xsd.
 */
public class ControlFormatoEnvio {

	final static Logger logger = LoggerFactory.getLogger(ControlFormatoEnvio.class);

	static final String XSD_DEFECTO = "resources/xsd/XSDs_FE_1_44_2/original/EnvioCFE.xsd";

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	public static Modo modo() {
		try {
			return Modo.valueOf(Play.application().configuration().getString("EnvioCFE.control.formato", Modo.LOG.name()).trim().toUpperCase());
		} catch (RuntimeException e) {
			return Modo.LOG;
		}
	}

	/**
	 * Valida el sobre firmado contra el XSD.
	 *
	 * @return null si cumple, si el control esta apagado o si no se pudo validar (eso no es culpa del sobre); si no, los
	 *         errores. En modo log ademas los deja en el log; quien llama decide si corta (modo enforce).
	 */
	public static String controlar(String xmlSobre, SobreEmitido sobre) {
		Modo modo = modo();
		if (modo == Modo.OFF)
			return null;
		try {
			Schema schema = ControlesRecepcion.schema("EnvioCFE.formato.xsd", XSD_DEFECTO);
			if (schema == null)
				return null;
			DocumentBuilderFactory dbf = XML.documentBuilderFactory();
			dbf.setNamespaceAware(true);
			Document doc = dbf.newDocumentBuilder().parse(new InputSource(new StringReader(xmlSobre)));
			String errores = ControlesRecepcion.validarXsd(schema, doc);
			if (errores != null)
				logger.warn("FORMATO SOBRE ({}) sobre id {} de {}: no cumple el XSD de DGI: {}", modo, sobre != null ? sobre.getId() : null,
						sobre != null && sobre.getEmpresaEmisora() != null ? sobre.getEmpresaEmisora().getRut() : null, errores);
			return errores;
		} catch (Throwable t) {
			logger.error("No se pudo controlar el formato del sobre " + (sobre != null ? sobre.getId() : null) + " (se manda igual)", t);
			return null;
		}
	}
}
