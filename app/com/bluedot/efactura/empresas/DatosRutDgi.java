package com.bluedot.efactura.empresas;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Respuesta del web service de consulta de RUT de DGI (WS_PersonaGetActEmpresarial, documento tecnico
 * T-5.020.00.001.006 "Servicios Web Externos DGI") pasada a JSON, con los campos principales:
 *
 * <pre>
 * { rut, denominacion, nombreFantasia, tipoEntidad: {codigo, descripcion}, estadoActividad: {codigo, descripcion},
 *   fechaInicioActividad, domicilioFiscal: {calle, numero, bis, apartamento, localidad, departamento, codigoPostal,
 *   comentario, complementos: [{tipo, valor}]}, contactos: [{tipo, valor}], actividades: [{codigo, nombre, desde}] }
 * </pre>
 */
public class DatosRutDgi {

	/** Estado de la actividad empresarial segun DGI */
	private static final Map<String, String> ESTADOS = new HashMap<String, String>();
	static {
		ESTADOS.put("AA", "Activo");
		ESTADOS.put("AF", "Activo a futuro");
		ESTADOS.put("CC", "Cancelado");
		ESTADOS.put("CH", "Cancelado hoy");
		ESTADOS.put("NT", "Nunca tuvo");
	}

	public static JSONObject aJson(String xml) throws Exception {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		DocumentBuilder builder = dbf.newDocumentBuilder();
		// Sin handler el parser escribe "[Fatal Error] ..." en la consola
		builder.setErrorHandler(new org.xml.sax.helpers.DefaultHandler() {
			@Override
			public void fatalError(org.xml.sax.SAXParseException e) throws org.xml.sax.SAXException {
				throw e;
			}
		});
		Document doc = builder.parse(new InputSource(new StringReader(xml.trim())));
		Element raiz = doc.getDocumentElement();

		JSONObject json = new JSONObject();
		json.put("rut", valor(texto(raiz, "RUT")));
		json.put("denominacion", valor(texto(raiz, "Denominacion")));
		json.put("nombreFantasia", valor(texto(raiz, "NombreFantasia")));
		json.put("tipoEntidad", codigo(texto(raiz, "TipoEntidad"), texto(raiz, "DescripcionTipoEntidad")));
		String estado = texto(raiz, "EstadoActividad");
		json.put("estadoActividad", codigo(estado, estado != null ? ESTADOS.get(estado) : null));
		// DGI escribe el tag con error: FechaInicioActivdad
		json.put("fechaInicioActividad", valor(fecha(texto(raiz, "FechaInicioActivdad"))));

		Element local = primero(raiz, "WS_PersonaActEmpresarial.WS_DomFiscalLocPrincipalItem");
		if (local != null) {
			JSONObject domicilio = new JSONObject();
			domicilio.put("calle", valor(texto(local, "Calle_Nom")));
			domicilio.put("numero", valor(texto(local, "Dom_Pta_Nro")));
			domicilio.put("bis", valor(texto(local, "Dom_Bis_Flg")));
			domicilio.put("apartamento", valor(sinCero(texto(local, "Dom_Ap_Nro"))));
			domicilio.put("localidad", valor(texto(local, "Loc_Nom")));
			domicilio.put("departamento", valor(texto(local, "Dpto_Nom")));
			domicilio.put("codigoPostal", valor(sinCero(texto(local, "Dom_Pst_Cod"))));
			domicilio.put("comentario", valor(texto(local, "Dom_Coment")));
			JSONArray complementos = new JSONArray();
			for (Element c : hijos(local, "WS_Domicilio.WS_DomicilioItem.Complemento"))
				complementos.put(new JSONObject().put("tipo", valor(texto(c, "Cmpl_Dsc"))).put("valor", valor(texto(c, "DomCmpl_Vlr"))));
			domicilio.put("complementos", complementos);
			json.put("domicilioFiscal", domicilio);

			JSONArray contactos = new JSONArray();
			for (Element c : hijos(local, "WS_Domicilio.WS_DomicilioItem.Contacto"))
				contactos.put(new JSONObject().put("tipo", valor(texto(c, "TipoCtt_Des"))).put("valor", valor(texto(c, "DomCtt_Val"))));
			json.put("contactos", contactos);
		} else {
			json.put("domicilioFiscal", JSONObject.NULL);
			json.put("contactos", new JSONArray());
		}

		JSONArray actividades = new JSONArray();
		for (Element a : hijos(raiz, "WS_PersonaActEmpresarial.WS_PersonaActividadesItem"))
			actividades.put(new JSONObject().put("codigo", valor(texto(a, "GiroCod"))).put("nombre", valor(texto(a, "GiroNom")))
					.put("desde", valor(fecha(texto(a, "GiroFec_Ini")))));
		json.put("actividades", actividades);

		return json;
	}

	private static JSONObject codigo(String codigo, String descripcion) {
		return new JSONObject().put("codigo", valor(codigo)).put("descripcion", valor(descripcion));
	}

	private static Object valor(String v) {
		return v != null ? v : JSONObject.NULL;
	}

	/** DGI manda 0000-00-00 cuando no hay fecha */
	private static String fecha(String f) {
		return f == null || f.startsWith("0000") ? null : f;
	}

	/** DGI manda 0 en los campos numericos vacios (apartamento, codigo postal) */
	private static String sinCero(String v) {
		return v == null || v.equals("0") ? null : v;
	}

	/** Texto del primer descendiente con ese nombre (sin importar el namespace); null si no esta o esta vacio */
	private static String texto(Element padre, String nombre) {
		Element e = primero(padre, nombre);
		if (e == null || e.getTextContent() == null)
			return null;
		String t = e.getTextContent().trim();
		return t.isEmpty() ? null : t;
	}

	private static Element primero(Element padre, String nombre) {
		NodeList l = padre.getElementsByTagNameNS("*", nombre);
		return l.getLength() > 0 ? (Element) l.item(0) : null;
	}

	private static java.util.List<Element> hijos(Element padre, String nombre) {
		java.util.List<Element> lista = new java.util.ArrayList<Element>();
		NodeList l = padre.getElementsByTagNameNS("*", nombre);
		for (int i = 0; i < l.getLength(); i++)
			if (l.item(i).getNodeType() == Node.ELEMENT_NODE)
				lista.add((Element) l.item(i));
		return lista;
	}
}
