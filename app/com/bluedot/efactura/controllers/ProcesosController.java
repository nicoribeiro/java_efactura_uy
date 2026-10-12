package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
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

	private static Object fecha(SimpleDateFormat sdf, Date date) {
		return date != null ? sdf.format(date) : JSONObject.NULL;
	}

	private static Object texto(String valor) {
		return valor != null ? valor : JSONObject.NULL;
	}
}
