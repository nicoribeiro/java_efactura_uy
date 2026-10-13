package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.model.EjecucionProceso;
import com.bluedot.efactura.pollers.EstadoPoller;
import com.bluedot.efactura.security.EmpresaAccess;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Estado de los procesos automaticos (pollers) de esta instancia: si estan activos, cuando corrieron y con que
 * resultado. Solo lectura, solo ADMIN (siempre). El estado es en memoria: cuenta desde que arranco la instancia. @Tx
 * hace falta igual: la autenticacion busca la sesion en la base.
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
public class ProcesosController extends AbstractController {

	public Promise<Result> getProcesos() throws APIException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

		JSONArray procesos = new JSONArray();
		for (EstadoPoller estado : EstadoPoller.todos()) {
			JSONObject json = new JSONObject();
			json.put("nombre", estado.getNombre());
			json.put("activo", estado.isActivo());
			json.put("intervaloSegundos", estado.getIntervaloMs() / 1000);
			json.put("programacion", texto(estado.getProgramacion()));
			json.put("ultimoInicio", fecha(sdf, estado.getUltimoInicio()));
			json.put("ultimoFin", fecha(sdf, estado.getUltimoFin()));
			json.put("ultimaDuracionMs", estado.getUltimoInicio() != null && estado.getUltimoFin() != null
					&& !estado.getUltimoFin().before(estado.getUltimoInicio())
							? estado.getUltimoFin().getTime() - estado.getUltimoInicio().getTime()
							: JSONObject.NULL);
			json.put("enEjecucion", estado.getUltimoInicio() != null
					&& (estado.getUltimoFin() == null || estado.getUltimoFin().before(estado.getUltimoInicio())));
			json.put("ejecuciones", estado.getEjecuciones());
			json.put("errores", estado.getErrores());
			json.put("ultimoError", texto(estado.getUltimoError()));
			json.put("ultimoErrorFecha", fecha(sdf, estado.getUltimoErrorFecha()));
			json.put("resumen", texto(estado.getResumen()));
			json.put("resumenFecha", fecha(sdf, estado.getResumenFecha()));
			procesos.put(json);
		}

		JSONObject result = new JSONObject();
		result.put("inicioInstancia", sdf.format(EstadoPoller.getInicio()));
		result.put("ahora", sdf.format(new Date()));
		result.put("procesos", procesos);
		return json(result.toString());
	}

	/**
	 * Ejecuciones registradas de un proceso (EjecucionProceso), de la mas nueva a la mas vieja. Query: page, pageSize
	 * (maximo 50). Solo ADMIN (siempre, por la ruta /procesos).
	 */
	public Promise<Result> getEjecuciones(String nombre) throws APIException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		int page = 1, pageSize = 20;
		try {
			if (request().getQueryString("page") != null)
				page = Math.max(1, Integer.parseInt(request().getQueryString("page")));
			if (request().getQueryString("pageSize") != null)
				pageSize = Math.max(1, Math.min(50, Integer.parseInt(request().getQueryString("pageSize"))));
		} catch (NumberFormatException e) {
			throw APIException.raise(com.bluedot.commons.error.APIException.APIErrors.BAD_PARAMETER_VALUE).withParams("page/pageSize");
		}
		com.bluedot.commons.utils.Tuple<java.util.List<EjecucionProceso>, Long> ejecuciones = EjecucionProceso.findByProceso(nombre, page, pageSize);
		JSONArray lista = new JSONArray();
		for (EjecucionProceso e : ejecuciones.item1) {
			JSONObject json = new JSONObject();
			json.put("id", e.getId());
			json.put("inicio", fecha(sdf, e.getInicio()));
			json.put("fin", fecha(sdf, e.getFin()));
			json.put("duracionMs", e.getInicio() != null && e.getFin() != null ? e.getFin().getTime() - e.getInicio().getTime() : JSONObject.NULL);
			json.put("ok", e.isOk());
			json.put("origen", texto(e.getOrigen()));
			json.put("resumen", texto(e.getResumen()));
			json.put("detalle", texto(e.getDetalle()));
			lista.put(json);
		}
		return json(com.bluedot.commons.utils.JSONUtils.createObjectList(lista, ejecuciones.item2, page, pageSize).toString());
	}

	private static Object fecha(SimpleDateFormat sdf, Date date) {
		return date != null ? sdf.format(date) : JSONObject.NULL;
	}

	private static Object texto(String valor) {
		return valor != null ? valor : JSONObject.NULL;
	}
}
