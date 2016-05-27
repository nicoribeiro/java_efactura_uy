package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import com.bluedot.commons.DateHandler;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.global.ErrorMessage;
import com.bluedot.efactura.global.Secured;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.soap.recepcion.Data;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class ReportController extends Controller {


	public Result generarReporteDiario(String fecha) throws EFacturaException {
		RecepcionService service = new RecepcionServiceImpl();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		Data response = service.generarReporteDiario(date);

		JSONObject jsonResponse = new JSONObject();
		
		if (response != null) {
			jsonResponse.put("xmlResponse", response.getXmlData());
			return ok(jsonResponse.toString()).as("application/json");
		} else
			throw EFacturaException.raise(EFacturaErrors.INTERNAL_SERVER_ERROR);
	}
}
