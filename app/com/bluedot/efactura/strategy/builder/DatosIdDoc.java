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
import dgi.classes.recepcion.IdDocResg;
import dgi.classes.recepcion.IdDocTck;

/**
 * Campos opcionales del IdDoc de los formatos de DGI 1.40+ que puede mandar la integracion en Encabezado.IdDoc del JSON
 * de emision (Formato_CFE v25-2, tipos en DGITypes.xsd 1.44.2):
 * <ul>
 * <li>e-Factura, e-Ticket y sus notas: IVAalDia (1-3), SecProf (1-2), IndPagCta3ros (1), IndCobPropia (1), ModVenta
 * (1,2,3,4,80,90,91,99), InfoAdicionalDoc (hasta 150)</li>
 * <li>e-Resguardo: FchValor (AAAA-MM-DD), SecProf (1-2), InfoAdicionalDoc (hasta 150)</li>
 * </ul>
 * Se validan al crear el CFE (antes de gastar el numero), se guardan en el CFE (datosIdDoc, JSON) y se ponen en el IdDoc
 * al armarlo para enviar. Sin ninguno el XML no cambia.
 */
public class DatosIdDoc {

	private static final Set<String> FACTURA_TICKET = new HashSet<String>(
			Arrays.asList("IVAalDia", "SecProf", "IndPagCta3ros", "IndCobPropia", "ModVenta", "InfoAdicionalDoc"));
	private static final Set<String> RESGUARDO = new HashSet<String>(Arrays.asList("FchValor", "SecProf", "InfoAdicionalDoc"));
	private static final Set<String> TODOS = new HashSet<String>(Arrays.asList("IVAalDia", "SecProf", "IndPagCta3ros", "IndCobPropia",
			"ModVenta", "InfoAdicionalDoc", "FchValor"));

	private static final Set<Integer> IVA_AL_DIA = new HashSet<Integer>(Arrays.asList(1, 2, 3));
	private static final Set<Integer> SEC_PROF = new HashSet<Integer>(Arrays.asList(1, 2));
	private static final Set<Integer> UNO = new HashSet<Integer>(Arrays.asList(1));
	private static final Set<Integer> MOD_VENTA = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 80, 90, 91, 99));

	/** Que campos admite el tipo de CFE (por su IdDoc), o null si ninguno */
	private static Set<String> admitidos(TipoDoc tipo) {
		int v = tipo.value % 100;
		if ((v >= 1 && v <= 3) || (v >= 11 && v <= 13) || (v >= 31 && v <= 33) || (v >= 41 && v <= 43))
			return FACTURA_TICKET;
		if (v == 82)
			return RESGUARDO;
		return null;
	}

	/**
	 * Lee y valida los campos que vengan en el IdDoc del JSON.
	 *
	 * @return el JSON con los que vinieron, o null si no vino ninguno
	 */
	public static String leer(TipoDoc tipo, JSONObject idDoc) throws APIException {
		JSONObject datos = new JSONObject();
		Set<String> admitidos = admitidos(tipo);
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
				datos.put(campo, entero(campo, valor, IVA_AL_DIA));
				break;
			case "SecProf":
				datos.put(campo, entero(campo, valor, SEC_PROF));
				break;
			case "IndPagCta3ros":
			case "IndCobPropia":
				datos.put(campo, entero(campo, valor, UNO));
				break;
			case "ModVenta":
				datos.put(campo, entero(campo, valor, MOD_VENTA));
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
