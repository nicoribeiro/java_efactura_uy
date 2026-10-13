package com.bluedot.efactura.controllers;

import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.DireccionDocumento;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.pollers.PollerManager;
import com.bluedot.efactura.recibidos.ControlesRecepcion;
import com.bluedot.efactura.recibidos.MontosRecibidos;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de Sobres") 
public class SobresController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(SobresController.class);

	@Inject
	public SobresController(PollerManager pollerManager){
		super();
	}
	
	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	/**
	 * Corrige los montos de los CFE recibidos con el XML de cada sobre: los totales (antes se calculaban a partir de las
	 * lineas, sin descuentos ni redondeos) y los descuentos y recargos de las lineas (antes no se guardaban). Solo ADMIN.
	 *
	 * Query: fromDate y toDate (segundos, opcionales, por fecha del sobre); guardar=1 para guardar, si no solo informa.
	 */
	public Promise<Result> corregirMontosRecibidos(String rut, int guardar) throws APIException {

		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "POST /empresa/" + rut + "/sobres/recibidos/corregirMontos", true);

		Empresa empresa = Empresa.findByRUT(rut, true);

		Date fromDate;
		Date toDate;
		try {
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate o toDate invalidos");
		}

		JSONArray corregidos = new JSONArray();
		int sobres = 0;
		for (int page = 1;; page++) {
			List<Sobre> pagina = Sobre.findByEmpresa(empresa, DireccionDocumento.RECIBIDO, null, fromDate, toDate, page, 200).item1;
			for (Sobre sobre : pagina) {
				sobres++;
				for (JSONObject corregido : MontosRecibidos.corregirSobre(sobre, guardar > 0)) {
					corregido.put("sobre", sobre.getId());
					corregido.put("emisor", sobre.getEmpresaEmisora() != null ? sobre.getEmpresaEmisora().getRut() : JSONObject.NULL);
					corregidos.put(corregido);
				}
			}
			if (pagina.size() < 200)
				break;
		}

		if (guardar > 0)
			auditoria.info(PermissionValidator.getSessionUser(ctx()).getEmailAddress() + " corrigió los montos de "
					+ corregidos.length() + " CFE recibidos de la empresa " + rut + " (" + sobres + " sobres revisados)");

		JSONObject resultado = new JSONObject();
		resultado.put("guardado", guardar > 0);
		resultado.put("sobresRevisados", sobres);
		resultado.put("cantidad", corregidos.length());
		resultado.put("corregidos", corregidos);
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/**
	 * Simula los controles de recepcion (E02, E03, E04, E07) sobre los CFE de los sobres recibidos ya guardados, sin
	 * cambiar nada: cuantos no pasarian cada control y ejemplos. Sirve para medir un control antes de activarlo. Solo
	 * ADMIN. Query: fromDate y toDate (segundos, por fecha del sobre), limite (sobres, por defecto 500).
	 */
	public Promise<Result> simularControles(String rut) throws APIException {
		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "GET /empresa/" + rut + "/sobres/recibidos/simularControles", true);
		Empresa empresa = Empresa.findByRUT(rut, true);

		Date fromDate;
		Date toDate;
		int limite;
		try {
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
			limite = request().getQueryString("limite") != null ? Integer.parseInt(request().getQueryString("limite")) : 500;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate, toDate o limite invalidos");
		}

		String[] controles = { "E02", "E03", "E04", "E07" };
		Map<String, Integer> fallan = new HashMap<String, Integer>();
		Map<String, JSONArray> ejemplos = new HashMap<String, JSONArray>();
		for (String c : controles) {
			fallan.put(c, 0);
			ejemplos.put(c, new JSONArray());
		}
		int sobres = 0, cfes = 0;

		List<Sobre> lista = Sobre.findByEmpresa(empresa, DireccionDocumento.RECIBIDO, null, fromDate, toDate, 1, Math.max(1, limite)).item1;
		for (Sobre sobre : lista) {
			sobres++;
			dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas envio = sobre.getEnvioCFEEntreEmpresas();
			if (envio == null)
				continue;
			int ordinal = 0;
			for (dgi.classes.entreEmpresas.CFEEmpresasType cfeEmpresas : envio.getCFEAdendas()) {
				ordinal++;
				dgi.classes.recepcion.CFEDefType xml = cfeEmpresas.getCFE();
				if (xml == null)
					continue;
				cfes++;
				CFE cfe = cfeDelSobre(sobre, xml);
				Map<String, String> resultado = new HashMap<String, String>();
				try {
					if (cfe != null)
						resultado.put("E02", ControlesRecepcion.e02(cfe, empresa));
				} catch (APIException e) {
					resultado.put("E02", "error: " + e.getMessage());
				}
				if (cfe != null)
					resultado.put("E03", ControlesRecepcion.e03(xml, cfe.getNro()));
				resultado.put("E04", ControlesRecepcion.e04(sobre.getXmlEmpresa(), ordinal, xml));
				resultado.put("E07", ControlesRecepcion.e07(xml));
				for (String c : controles) {
					String detalle = resultado.get(c);
					if (detalle == null)
						continue;
					fallan.put(c, fallan.get(c) + 1);
					if (ejemplos.get(c).length() < 20) {
						JSONObject ej = new JSONObject();
						ej.put("sobre", sobre.getId());
						ej.put("archivo", sobre.getNombreArchivo());
						ej.put("cfe", cfe != null ? cfe.getTipo().value + " " + cfe.getSerie() + "-" + cfe.getNro() : "(no guardado)");
						ej.put("detalle", detalle);
						ejemplos.get(c).put(ej);
					}
				}
			}
		}

		JSONObject resumen = new JSONObject();
		resumen.put("sobres", sobres);
		resumen.put("cfes", cfes);
		JSONObject porControl = new JSONObject();
		for (String c : controles) {
			JSONObject r = new JSONObject();
			r.put("noPasan", fallan.get(c));
			r.put("ejemplos", ejemplos.get(c));
			porControl.put(c, r);
		}
		resumen.put("controles", porControl);
		return json(JSONUtils.merge(resumen, new JSONObject(OK)).toString());
	}

	/** El CFE guardado que corresponde a un CFE del XML del sobre (por tipo, serie y numero) */
	private static CFE cfeDelSobre(Sobre sobre, dgi.classes.recepcion.CFEDefType xml) {
		java.math.BigInteger tipo = null, nro = null;
		String serie = null;
		if (xml.getEFact() != null) {
			tipo = xml.getEFact().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEFact().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEFact().getEncabezado().getIdDoc().getNro();
		} else if (xml.getETck() != null) {
			tipo = xml.getETck().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getETck().getEncabezado().getIdDoc().getSerie();
			nro = xml.getETck().getEncabezado().getIdDoc().getNro();
		} else if (xml.getERem() != null) {
			tipo = xml.getERem().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getERem().getEncabezado().getIdDoc().getSerie();
			nro = xml.getERem().getEncabezado().getIdDoc().getNro();
		} else if (xml.getEResg() != null) {
			tipo = xml.getEResg().getEncabezado().getIdDoc().getTipoCFE();
			serie = xml.getEResg().getEncabezado().getIdDoc().getSerie();
			nro = xml.getEResg().getEncabezado().getIdDoc().getNro();
		}
		if (tipo == null || nro == null)
			return null;
		return sobre.getCFE(nro.longValue(), serie, com.bluedot.efactura.model.TipoDoc.fromInt(tipo.intValue()));
	}

	public Promise<Result> enviarSobreEmpresa(String rut, long sobreId) throws APIException {

		Sobre sobre = sobreDeLaEmpresa(Empresa.findByRUT(rut, true), sobreId);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		JSONObject error = null;
		
		if (sobre instanceof SobreEmitido)
			try {
				factory.getServiceMicroController(sobre.getEmpresaEmisora()).enviarCorreoReceptorElectronico((SobreEmitido)sobre);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}
		
		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}
	
	
	public Promise<Result> getSobre(String rut, long sobreId) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Sobre sobre = sobreDeLaEmpresa(empresa, sobreId);
		boolean emitido = sobre instanceof SobreEmitido;

		JSONObject cfeJson;
		try {	
			cfeJson = EfacturaJSONSerializerProvider.getSobreSerializer().objectToJson(sobre);
			cfeJson.put("direccion", emitido ? DireccionDocumento.EMITIDO.name() : DireccionDocumento.RECIBIDO.name());
			// Comprobantes del sobre identificados por tipo, serie y numero (cfes solo trae los id internos)
			JSONArray comprobantes = new JSONArray();
			for (CFE cfe : sobre.getCfes())
				comprobantes.put(resumenCfe(cfe));
			cfeJson.put("comprobantes", comprobantes);
		}catch (JSONException e) {
			throw APIException.raise(e);
		}
		
		return json(cfeJson.toString());
		
	}

	/**
	 * Sobres de la empresa (emitidos o recibidos), paginados, con sus comprobantes resumidos. Solo lectura.
	 * 
	 * Parametros: direccion (EMITIDO por defecto), fromDate y toDate (epoch en segundos), estado (AS, BS o SIN), page y
	 * pageSize (maximo 50).
	 */
	public Promise<Result> getSobres(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		DireccionDocumento direccion;
		Date fromDate;
		Date toDate;
		int page;
		int pageSize;
		try {
			direccion = request().getQueryString("direccion") != null ? DireccionDocumento.valueOf(request().getQueryString("direccion")) : DireccionDocumento.EMITIDO;
			fromDate = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000) : null;
			toDate = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : null;
			page = request().getQueryString("page") != null ? Integer.parseInt(request().getQueryString("page")) : 1;
			pageSize = request().getQueryString("pageSize") != null ? Integer.parseInt(request().getQueryString("pageSize")) : 20;
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("direccion, fromDate, toDate, page o pageSize invalidos");
		}
		if (page <= 0)
			page = 1;
		if (pageSize <= 0)
			pageSize = 20;
		pageSize = Math.min(pageSize, 50);

		String estado = request().getQueryString("estado");
		if (estado != null && !estado.trim().isEmpty() && !estado.trim().matches("(?i)AS|BS|SIN"))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("estado", estado);

		Tuple<List<Sobre>, Long> sobres = Sobre.findByEmpresa(empresa, direccion, estado, fromDate, toDate, page, pageSize);

		// Comprobantes de todos los sobres de la pagina en una sola consulta
		List<Long> ids = new LinkedList<Long>();
		for (Sobre sobre : sobres.item1)
			ids.add(sobre.getId());
		Map<Long, JSONArray> comprobantes = new HashMap<Long, JSONArray>();
		for (CFE cfe : CFE.findBySobres(ids, direccion)) {
			Sobre sobre = direccion == DireccionDocumento.EMITIDO ? cfe.getSobreEmitido() : cfe.getSobreRecibido();
			if (!comprobantes.containsKey(sobre.getId()))
				comprobantes.put(sobre.getId(), new JSONArray());
			comprobantes.get(sobre.getId()).put(resumenCfe(cfe));
		}

		JSONArray lista = new JSONArray();
		for (Sobre sobre : sobres.item1)
			lista.put(resumenSobre(sobre, comprobantes.containsKey(sobre.getId()) ? comprobantes.get(sobre.getId()) : new JSONArray()));

		return json(JSONUtils.createObjectList(lista, sobres.item2, page, pageSize).toString());
	}

	/**
	 * Sobre por id, solo si es de la empresa del path (lo emitio o lo recibio). Si no, SOBRE_NO_ENCONTRADO, como si no
	 * existiera: con acceso a una empresa no se pueden ver ni reenviar sobres de otra.
	 */
	private static Sobre sobreDeLaEmpresa(Empresa empresa, long sobreId) throws APIException {
		Sobre sobre = Sobre.findById(sobreId, true);
		Empresa duenia = sobre instanceof SobreEmitido ? sobre.getEmpresaEmisora() : sobre.getEmpresaReceptora();
		if (duenia == null || duenia.getId() != empresa.getId())
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO).withParams("id", sobreId);
		return sobre;
	}

	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

	private static Object fecha(Date date) {
		return date != null ? sdf.format(date) : JSONObject.NULL;
	}

	private static Object empresaRef(Empresa empresa) {
		if (empresa == null)
			return JSONObject.NULL;
		JSONObject json = new JSONObject();
		json.put("rut", empresa.getRut());
		json.put("razon", empresa.getRazon());
		return json;
	}

	/** Sobre para listados: sin XML ni respuestas completas */
	private static JSONObject resumenSobre(Sobre sobre, JSONArray comprobantes) {
		JSONObject json = new JSONObject();
		json.put("id", sobre.getId());
		json.put("fecha", fecha(sobre.getFecha()));
		json.put("nombreArchivo", sobre.getNombreArchivo());
		json.put("cantComprobantes", sobre.getCantComprobantes());
		json.put("estadoEmpresa", sobre.getEstadoEmpresa() != null ? sobre.getEstadoEmpresa().name() : JSONObject.NULL);
		json.put("empresaEmisora", empresaRef(sobre.getEmpresaEmisora()));
		json.put("empresaReceptora", empresaRef(sobre.getEmpresaReceptora()));
		if (sobre instanceof SobreEmitido) {
			SobreEmitido emitido = (SobreEmitido) sobre;
			json.put("direccion", DireccionDocumento.EMITIDO.name());
			json.put("estadoDgi", emitido.getEstadoDgi() != null ? emitido.getEstadoDgi().name() : JSONObject.NULL);
			json.put("fechaConsulta", fecha(emitido.getFechaConsulta()));
		} else if (sobre instanceof SobreRecibido) {
			SobreRecibido recibido = (SobreRecibido) sobre;
			json.put("direccion", DireccionDocumento.RECIBIDO.name());
			json.put("timestampRecibido", fecha(recibido.getTimestampRecibido()));
			json.put("timestampProcesado", fecha(recibido.getTimestampProcesado()));
		}
		json.put("comprobantes", comprobantes);
		return json;
	}

	/** Comprobante identificado por tipo, serie y numero, con lo necesario para listarlo y enlazarlo */
	private static JSONObject resumenCfe(CFE cfe) {
		JSONObject json = new JSONObject();
		json.put("id", cfe.getId());
		json.put("tipo", cfe.getTipo().name());
		json.put("idTipoDoc", cfe.getTipo().value);
		json.put("serie", cfe.getSerie());
		json.put("nro", cfe.getNro());
		json.put("fechaEmision", fecha(cfe.getFechaEmision()));
		json.put("moneda", cfe.getMoneda() != null ? cfe.getMoneda().name() : JSONObject.NULL);
		json.put("totMntTotal", cfe.getTotMntTotal() != null ? cfe.getTotMntTotal() : JSONObject.NULL);
		json.put("estado", cfe.getEstado() != null ? cfe.getEstado().name() : JSONObject.NULL);
		json.put("empresaEmisora", empresaRef(cfe.getEmpresaEmisora()));
		json.put("empresaReceptora", empresaRef(cfe.getEmpresaReceptora()));
		json.put("titular", cfe.getTitular() != null ? cfe.getTitular().getDocumento() : JSONObject.NULL);
		return json;
	}
	
	
	
	
}
