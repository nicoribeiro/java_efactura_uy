package com.bluedot.efactura.controllers;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.model.TipoDeCambio;
import com.bluedot.efactura.model.UI;
import com.play4jpa.jpa.db.Tx;

import dgi.classes.recepcion.TipMonType;
import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Valores de referencia que usa el sistema, solo lectura: cotizaciones (las descarga PollerTipoDeCambioRunner de INE) y
 * Unidad Indexada por anio. La emision de eTickets en moneda extranjera necesita la cotizacion del dia anterior y la UI
 * del anio (tope de 5000 UI).
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@Api(value = "Cotizaciones y UI")
public class CotizacionesController extends AbstractController {

	/** Rango maximo por consulta, para no traer anios de cotizaciones de una vez */
	private static final int MAX_DIAS = 400;

	/**
	 * Cotizaciones de una moneda (USD por defecto) entre desde y hasta (yyyy-MM-dd, inclusive). Por defecto los ultimos 30
	 * dias.
	 */
	public Promise<Result> getTiposDeCambio() throws APIException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		sdf.setLenient(false);

		TipMonType moneda;
		Date desde;
		Date hasta;
		try {
			moneda = request().getQueryString("moneda") != null ? TipMonType.valueOf(request().getQueryString("moneda").toUpperCase()) : TipMonType.USD;
			Calendar hoy = Calendar.getInstance();
			hasta = request().getQueryString("hasta") != null ? sdf.parse(request().getQueryString("hasta")) : sdf.parse(sdf.format(hoy.getTime()));
			if (request().getQueryString("desde") != null)
				desde = sdf.parse(request().getQueryString("desde"));
			else {
				Calendar c = Calendar.getInstance();
				c.setTime(hasta);
				c.add(Calendar.DAY_OF_MONTH, -29);
				desde = c.getTime();
			}
		} catch (IllegalArgumentException | ParseException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("moneda, desde o hasta invalidos (fechas yyyy-MM-dd)");
		}

		long dias = (hasta.getTime() - desde.getTime()) / (24L * 60 * 60 * 1000) + 1;
		if (dias <= 0 || dias > MAX_DIAS)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("desde/hasta")
					.setDetailMessage("El periodo debe tener entre 1 y " + MAX_DIAS + " dias");

		JSONArray cotizaciones = new JSONArray();
		for (TipoDeCambio tc : TipoDeCambio.findByMonedaEntre(moneda, desde, hasta)) {
			JSONObject json = new JSONObject();
			json.put("fecha", sdf.format(tc.getFecha()));
			json.put("compra", tc.getCompra() != null ? tc.getCompra().doubleValue() : JSONObject.NULL);
			json.put("venta", tc.getVenta() != null ? tc.getVenta().doubleValue() : JSONObject.NULL);
			json.put("interbancario", tc.getInterbancario() != null ? tc.getInterbancario().doubleValue() : JSONObject.NULL);
			cotizaciones.put(json);
		}

		JSONObject resultado = new JSONObject();
		resultado.put("moneda", moneda.name());
		resultado.put("desde", sdf.format(desde));
		resultado.put("hasta", sdf.format(hasta));
		resultado.put("cotizaciones", cotizaciones);
		return json(resultado.toString());
	}

	/** Valor de la Unidad Indexada por anio (el que usa el tope de 5000 UI de los eTickets) */
	public Promise<Result> getUI() throws APIException {
		JSONArray lista = new JSONArray();
		for (UI ui : UI.findAll()) {
			JSONObject json = new JSONObject();
			json.put("anio", ui.getAnio());
			json.put("cotizacion", ui.getCotizacion());
			lista.put(json);
		}
		return json(lista.toString());
	}
}
