package com.bluedot.efactura.strategy.builder;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.IdDocFact;
import dgi.classes.recepcion.IdDocFactExp;
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

/**
 * Campos opcionales del IdDoc de los formatos de DGI 1.40+ que puede mandar la integracion en Encabezado.IdDoc del JSON
 * de emision (Formato_CFE v25-2, tipos en DGITypes.xsd 1.44.2):
 * <ul>
 * <li>e-Factura, e-Ticket y sus notas: IVAalDia (1-3), SecProf (1-2), IndPagCta3ros (1), IndCobPropia (1), ModVenta
 * (1,2,3,4,80,90,91,99), InfoAdicionalDoc (hasta 150)</li>
 * <li>e-Resguardo: FchValor (AAAA-MM-DD), SecProf (1-2), InfoAdicionalDoc (hasta 150)</li>
 * <li>e-Factura de Exportacion y sus notas (121-123): ClauVenta (A-C13, obligatorio: Incoterm de hasta 3 letras o N/A),
 * ModVenta (A-C14: 1,2,3,4,90,91,99; obligatorio en la e-Factura 121, opcional en las notas), ViaTransp (A-C15,
 * obligatorio: 1,2,3,8,9), InfoAdicionalDoc, IVAalDia (1), SecProf (1), IndPagCta3ros (1)</li>
 * </ul>
 * Se validan al crear el CFE (antes de gastar el numero), se guardan en el CFE (datosIdDoc, JSON) y se ponen en el IdDoc
 * al armarlo para enviar. Sin ninguno el XML no cambia.
 */
public class DatosIdDoc {

	private static final Set<String> FACTURA_TICKET = new HashSet<String>(
			Arrays.asList("IVAalDia", "SecProf", "IndPagCta3ros", "IndCobPropia", "ModVenta", "InfoAdicionalDoc"));
	private static final Set<String> RESGUARDO = new HashSet<String>(Arrays.asList("FchValor", "SecProf", "InfoAdicionalDoc"));
	private static final Set<String> EXPORTACION = new HashSet<String>(
			Arrays.asList("ClauVenta", "ModVenta", "ViaTransp", "InfoAdicionalDoc", "IVAalDia", "SecProf", "IndPagCta3ros"));
	private static final Set<String> TODOS = new HashSet<String>(Arrays.asList("IVAalDia", "SecProf", "IndPagCta3ros", "IndCobPropia",
			"ModVenta", "InfoAdicionalDoc", "FchValor", "ClauVenta", "ViaTransp"));

	private static final Set<Integer> IVA_AL_DIA = new HashSet<Integer>(Arrays.asList(1, 2, 3));
	private static final Set<Integer> SEC_PROF = new HashSet<Integer>(Arrays.asList(1, 2));
	private static final Set<Integer> UNO = new HashSet<Integer>(Arrays.asList(1));
	private static final Set<Integer> MOD_VENTA = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 80, 90, 91, 99));
	// Exportacion (A-C14): 80 (Tax Free) es solo para e-Ticket
	private static final Set<Integer> MOD_VENTA_EXPORTACION = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 90, 91, 99));
	// Via de transporte (A-C15): 1 maritimo, 2 aereo, 3 terrestre, 8 N/A, 9 otro
	private static final Set<Integer> VIA_TRANSP = new HashSet<Integer>(Arrays.asList(1, 2, 3, 8, 9));

	/** Que campos admite el tipo de CFE (por su IdDoc), o null si ninguno */
	private static Set<String> admitidos(TipoDoc tipo) {
		int v = tipo.value % 100;
		if ((v >= 1 && v <= 3) || (v >= 11 && v <= 13) || (v >= 31 && v <= 33) || (v >= 41 && v <= 43))
			return FACTURA_TICKET;
		if (v == 82)
			return RESGUARDO;
		if (esExportacion(tipo))
			return EXPORTACION;
		return null;
	}

	/** e-Factura de Exportacion y sus notas (121-123 y 221-223); el e-Remito de exportacion (124) no */
	public static boolean esExportacion(TipoDoc tipo) {
		int v = tipo.value % 100;
		return v >= 21 && v <= 23;
	}

	/**
	 * Lee y valida los campos que vengan en el IdDoc del JSON.
	 *
	 * @return el JSON con los que vinieron, o null si no vino ninguno
	 */
	public static String leer(TipoDoc tipo, JSONObject idDoc) throws APIException {
		JSONObject datos = new JSONObject();
		Set<String> admitidos = admitidos(tipo);
		boolean exportacion = esExportacion(tipo);
		// IndPagCta3ros: tambien se acepta como IndPagCta3Ros
		if (idDoc.has("IndPagCta3Ros") && !idDoc.has("IndPagCta3ros"))
			idDoc.put("IndPagCta3ros", idDoc.get("IndPagCta3Ros"));
		for (String campo : TODOS) {
			if (!idDoc.has(campo) || idDoc.isNull(campo) || idDoc.get(campo).toString().trim().isEmpty())
				continue;
			if (admitidos == null || !admitidos.contains(campo))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, idDoc.get(campo))
						.setDetailMessage(campo + " no corresponde a " + tipo.friendlyName);
			String valor = idDoc.get(campo).toString().trim();
			switch (campo) {
			case "IVAalDia":
				// 2 y 3 (mandante) solo en venta por cuenta ajena
				datos.put(campo, entero(campo, valor, exportacion ? UNO : IVA_AL_DIA));
				break;
			case "SecProf":
				// 2 solo en venta por cuenta ajena
				datos.put(campo, entero(campo, valor, exportacion ? UNO : SEC_PROF));
				break;
			case "IndPagCta3ros":
			case "IndCobPropia":
				datos.put(campo, entero(campo, valor, UNO));
				break;
			case "ModVenta":
				datos.put(campo, entero(campo, valor, exportacion ? MOD_VENTA_EXPORTACION : MOD_VENTA));
				break;
			case "ViaTransp":
				datos.put(campo, entero(campo, valor, VIA_TRANSP));
				break;
			case "ClauVenta":
				// XSD: hasta 3 letras (Incoterm: FOB, CIF, EXW...) o N/A si no corresponde
				valor = valor.toUpperCase();
				if (!valor.matches("[A-Z]{1,3}") && !valor.equals("N/A"))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
							.setDetailMessage("ClauVenta tiene que ser la clausula de venta (Incoterm de hasta 3 letras, ej. FOB, CIF, EXW) o N/A si no corresponde");
				datos.put(campo, valor);
				break;
			case "InfoAdicionalDoc":
				if (valor.length() > 150)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
							.setDetailMessage("InfoAdicionalDoc admite hasta 150 caracteres");
				datos.put(campo, valor);
				break;
			case "FchValor":
				try {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
					sdf.setLenient(false);
					sdf.parse(valor);
				} catch (ParseException e) {
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
							.setDetailMessage("FchValor tiene que ser una fecha AAAA-MM-DD");
				}
				datos.put(campo, valor);
				break;
			default:
			}
		}
		if (exportacion) {
			// Formato CFE: A-C13 y A-C15 obligatorios en la e-Factura de exportacion y sus notas; A-C14 en la e-Factura
			if (!datos.has("ClauVenta"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("IdDoc.ClauVenta")
						.setDetailMessage("Falta la clausula de venta (ClauVenta: Incoterm, ej. FOB, CIF, EXW, o N/A si no corresponde)");
			if (!datos.has("ViaTransp"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("IdDoc.ViaTransp")
						.setDetailMessage("Falta la via de transporte (ViaTransp: 1 maritimo, 2 aereo, 3 terrestre, 8 no aplica, 9 otro)");
			if (tipo.value % 100 == 21 && !datos.has("ModVenta"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("IdDoc.ModVenta")
						.setDetailMessage("Falta la modalidad de venta (ModVenta: 1, 2, 3, 4, 90, 91 o 99), obligatoria en la e-Factura de exportacion");
		}
		return datos.length() > 0 ? datos.toString() : null;
	}

	private static int entero(String campo, String valor, Set<Integer> validos) throws APIException {
		try {
			int n = Integer.parseInt(valor);
			if (validos.contains(n))
				return n;
		} catch (NumberFormatException e) {
		}
		throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
				.setDetailMessage(campo + " admite " + new java.util.TreeSet<Integer>(validos));
	}

	public static void aplicar(String json, IdDocFact idDoc) {
		if (json == null)
			return;
		JSONObject d = new JSONObject(json);
		idDoc.setIVAalDia(d.has("IVAalDia") ? BigInteger.valueOf(d.getInt("IVAalDia")) : null);
		idDoc.setSecProf(d.has("SecProf") ? BigInteger.valueOf(d.getInt("SecProf")) : null);
		idDoc.setIndPagCta3Ros(d.has("IndPagCta3ros") ? BigInteger.valueOf(d.getInt("IndPagCta3ros")) : null);
		idDoc.setIndCobPropia(d.has("IndCobPropia") ? BigInteger.valueOf(d.getInt("IndCobPropia")) : null);
		idDoc.setModVenta(d.has("ModVenta") ? d.getInt("ModVenta") : null);
		idDoc.setInfoAdicionalDoc(d.has("InfoAdicionalDoc") ? d.getString("InfoAdicionalDoc") : null);
	}

	public static void aplicar(String json, IdDocTck idDoc) {
		if (json == null)
			return;
		JSONObject d = new JSONObject(json);
		idDoc.setIVAalDia(d.has("IVAalDia") ? BigInteger.valueOf(d.getInt("IVAalDia")) : null);
		idDoc.setSecProf(d.has("SecProf") ? BigInteger.valueOf(d.getInt("SecProf")) : null);
		idDoc.setIndPagCta3Ros(d.has("IndPagCta3ros") ? BigInteger.valueOf(d.getInt("IndPagCta3ros")) : null);
		idDoc.setIndCobPropia(d.has("IndCobPropia") ? BigInteger.valueOf(d.getInt("IndCobPropia")) : null);
		idDoc.setModVenta(d.has("ModVenta") ? d.getInt("ModVenta") : null);
		idDoc.setInfoAdicionalDoc(d.has("InfoAdicionalDoc") ? d.getString("InfoAdicionalDoc") : null);
	}

	public static void aplicar(String json, IdDocFactExp idDoc) {
		if (json == null)
			return;
		JSONObject d = new JSONObject(json);
		idDoc.setClauVenta(d.has("ClauVenta") ? d.getString("ClauVenta") : null);
		idDoc.setModVenta(d.has("ModVenta") ? d.getInt("ModVenta") : null);
		if (d.has("ViaTransp"))
			idDoc.setViaTransp(d.getInt("ViaTransp"));
		idDoc.setInfoAdicionalDoc(d.has("InfoAdicionalDoc") ? d.getString("InfoAdicionalDoc") : null);
		idDoc.setIVAalDia(d.has("IVAalDia") ? BigInteger.valueOf(d.getInt("IVAalDia")) : null);
		idDoc.setSecProf(d.has("SecProf") ? BigInteger.valueOf(d.getInt("SecProf")) : null);
		idDoc.setIndPagCta3Ros(d.has("IndPagCta3ros") ? BigInteger.valueOf(d.getInt("IndPagCta3ros")) : null);
	}

	public static void aplicar(String json, IdDocResg idDoc) throws APIException {
		if (json == null)
			return;
		JSONObject d = new JSONObject(json);
		idDoc.setSecProf(d.has("SecProf") ? BigInteger.valueOf(d.getInt("SecProf")) : null);
		idDoc.setInfoAdicionalDoc(d.has("InfoAdicionalDoc") ? d.getString("InfoAdicionalDoc") : null);
		try {
			idDoc.setFchValor(d.has("FchValor") ? DatatypeFactory.newInstance().newXMLGregorianCalendar(d.getString("FchValor")) : null);
		} catch (DatatypeConfigurationException e) {
			throw APIException.raise(e);
		}
	}
}
