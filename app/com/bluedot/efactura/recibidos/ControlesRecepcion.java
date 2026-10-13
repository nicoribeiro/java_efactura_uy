package com.bluedot.efactura.recibidos;

import java.math.BigInteger;

import com.bluedot.efactura.model.CFE;

import dgi.classes.recepcion.CAEDataType;
import dgi.classes.recepcion.CFEDefType;
import play.Play;

/**
 * Controles de DGI sobre los CFE recibidos (E01-E07). Cada control tiene un modo en application.conf
 * (RecepcionCFE.control.EXX):
 * <ul>
 * <li>OFF: no se controla</li>
 * <li>LOG (por defecto): si no pasa se registra en el CFE (observacionesRecepcion) y en el log, pero se acepta igual</li>
 * <li>ENFORCE: si no pasa se rechaza el CFE con ese motivo (el ACK al emisor lo informa)</li>
 * </ul>
 * Se arranca en LOG porque un rechazo le envia una respuesta negativa al proveedor: primero se observa que rechazaria.
 */
public class ControlesRecepcion {

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	public static Modo modo(String control) {
		String valor = Play.application().configuration().getString("RecepcionCFE.control." + control, Modo.LOG.name());
		try {
			return Modo.valueOf(valor.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return Modo.LOG;
		}
	}

	/** CAEData del CFE, sea cual sea el tipo; null si no tiene */
	static CAEDataType caeData(CFEDefType xml) {
		if (xml == null)
			return null;
		if (xml.getEFact() != null)
			return xml.getEFact().getCAEData();
		if (xml.getETck() != null)
			return xml.getETck().getCAEData();
		if (xml.getERem() != null)
			return xml.getERem().getCAEData();
		if (xml.getEResg() != null)
			return xml.getEResg().getCAEData();
		if (xml.getEFactExp() != null)
			return xml.getEFactExp().getCAEData();
		if (xml.getERemExp() != null)
			return xml.getERemExp().getCAEData();
		return null;
	}

	/**
	 * E03: el numero del CFE tiene que estar en el rango del CAE con el que se emitio (CAEData: DNro a HNro).
	 *
	 * @return null si pasa; si no, el detalle de por que no
	 */
	public static String e03(CFEDefType xml, long nro) {
		CAEDataType cae = caeData(xml);
		if (cae == null)
			return "el CFE no trae los datos del CAE (CAEData)";
		BigInteger desde = cae.getDNro();
		BigInteger hasta = cae.getHNro();
		if (desde == null || hasta == null)
			return "el CAE " + cae.getCAEID() + " no indica el rango de números";
		BigInteger numero = BigInteger.valueOf(nro);
		if (numero.compareTo(desde) < 0 || numero.compareTo(hasta) > 0)
			return "el número " + nro + " está fuera del rango del CAE " + cae.getCAEID() + " (" + desde + " a " + hasta + ")";
		return null;
	}

	/** Agrega una observacion de control a las del CFE ("E03: ...; E07: ...") */
	public static void observar(CFE cfe, String control, String detalle) {
		String linea = control + ": " + detalle;
		String actuales = cfe.getObservacionesRecepcion();
		cfe.setObservacionesRecepcion(actuales == null || actuales.isEmpty() ? linea : actuales + "; " + linea);
	}
}
