package com.bluedot.efactura.controllers;

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
