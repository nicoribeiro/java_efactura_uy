package com.bluedot.efactura.recibidos;

import java.math.BigInteger;
import java.time.LocalDate;

import javax.xml.datatype.XMLGregorianCalendar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;

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

	/**
	 * E02: el CFE (emisor, tipo, serie y numero) ya se habia recibido y aceptado. No cuenta al CFE que se esta procesando
	 * (ya esta guardado cuando se controla).
	 *
	 * @return null si pasa; si no, el detalle con las copias anteriores
	 */
	public static String e02(CFE cfe, Empresa empresaReceptora) throws APIException {
		List<CFE> anteriores = new ArrayList<CFE>();
		for (CFE otro : CFE.findByIdRecibido(empresaReceptora, cfe.getTipo(), cfe.getSerie(), cfe.getNro(), EstadoACKCFEType.AE,
				cfe.getEmpresaEmisora(), false))
			if (otro.getId() != cfe.getId())
				anteriores.add(otro);
		if (anteriores.isEmpty())
			return null;
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		StringBuilder copias = new StringBuilder();
		for (CFE otro : anteriores) {
			if (copias.length() > 0)
				copias.append(", ");
			copias.append("id ").append(otro.getId());
			if (otro.getSobreRecibido() != null && otro.getSobreRecibido().getFecha() != null)
				copias.append(" (sobre del ").append(sdf.format(otro.getSobreRecibido().getFecha())).append(")");
		}
		return "ya se había recibido y aceptado: " + copias;
	}

	/** Fecha y hora de firma del CFE (TmstFirma), sea cual sea el tipo; null si no tiene */
	static XMLGregorianCalendar tmstFirma(CFEDefType xml) {
		if (xml == null)
			return null;
		if (xml.getEFact() != null)
			return xml.getEFact().getTmstFirma();
		if (xml.getETck() != null)
			return xml.getETck().getTmstFirma();
		if (xml.getERem() != null)
			return xml.getERem().getTmstFirma();
		if (xml.getEResg() != null)
			return xml.getEResg().getTmstFirma();
		if (xml.getEFactExp() != null)
			return xml.getEFactExp().getTmstFirma();
		if (xml.getERemExp() != null)
			return xml.getERemExp().getTmstFirma();
		return null;
	}

	private static LocalDate dia(XMLGregorianCalendar fecha) {
		return LocalDate.of(fecha.getYear(), fecha.getMonth(), fecha.getDay());
	}

	/**
	 * E07: el CFE se tiene que haber firmado con el CAE vigente: la fecha de firma (TmstFirma, el dia en la zona horaria
	 * de la firma) no puede ser posterior al vencimiento del CAE (CAEData FecVenc).
	 *
	 * @return null si pasa; si no, el detalle de por que no
	 */
	public static String e07(CFEDefType xml) {
		CAEDataType cae = caeData(xml);
		if (cae == null || cae.getFecVenc() == null)
			return "el CAE no indica la fecha de vencimiento";
		XMLGregorianCalendar firma = tmstFirma(xml);
		if (firma == null)
			return "el CFE no trae la fecha de firma (TmstFirma)";
		LocalDate diaFirma = dia(firma);
		LocalDate vencimiento = dia(cae.getFecVenc());
		if (diaFirma.isAfter(vencimiento))
			return "firmado el " + diaFirma + ", después del vencimiento del CAE " + cae.getCAEID() + " (" + vencimiento + ")";
		return null;
	}

	/** Agrega una observacion de control a las del CFE ("E03: ...; E07: ...") */
	public static void observar(CFE cfe, String control, String detalle) {
		String linea = control + ": " + detalle;
		String actuales = cfe.getObservacionesRecepcion();
		cfe.setObservacionesRecepcion(actuales == null || actuales.isEmpty() ? linea : actuales + "; " + linea);
	}
}
