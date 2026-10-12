package com.bluedot.efactura.controllers;

import com.bluedot.efactura.security.EmpresaAccess;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.pollers.PollerManager;
import com.bluedot.efactura.reportes.ReportesDiarios;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de Reportes") 
public class ReportController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(ReportController.class);
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
	@Inject
	public ReportController(PollerManager pollerManager){
		super();
	}
	
	@ApiOperation(value = "Generar Reporte Diario",
		    response = ReporteDiario.class
		    )
	public Promise<Result> generarReporteDiario(String rut, String fecha, int cantReportes, int soloFaltantes) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);
		Date date = DateHandler.fromStringToDate(fecha, sdf);

		/*
		 * Body: Output (opcional) es la carpeta donde guardar el XML de cada reporte
		 */
		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());
		String output = document.has("Output") ? document.getString("Output") : null;

		JSONArray reportes = ReportesDiarios.generar(empresa, date, cantReportes, soloFaltantes > 0, output);

		return json(reportes.toString());
	}

	public Promise<Result> getReporteDiario(String rut, String fecha, int cantReportes) throws APIException {

		if (cantReportes < 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("cantReportes", cantReportes);

		Empresa empresa = Empresa.findByRUT(rut, true);

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		JSONArray reportes = new JSONArray();
		
		Date now = new Date();

		for (int i = 0; i < cantReportes; i++) {
			
			if (DateHandler.add(date, i, Calendar.DAY_OF_MONTH).after(now))
				break;
			
			logger.debug("Buscado reporte diario empresa:{} dia:{}", empresa.getRut(), sdf.format(DateHandler.add(date, i, Calendar.DAY_OF_MONTH)));
			
			List<ReporteDiario> reportesList = ReporteDiario.findByEmpresaFecha(empresa, DateHandler.add(date, i, Calendar.DAY_OF_MONTH));
			
			logger.debug("Serializando reporte diario empresa:{} dia:{}", empresa.getRut(), sdf.format(DateHandler.add(date, i, Calendar.DAY_OF_MONTH)));
			
			JSONArray jsonArray = EfacturaJSONSerializerProvider.getReporteDiarioSerializer().objectToJson(reportesList);
			
			if (jsonArray.length()>0)
				reportes.put(jsonArray);

		}

		return json(reportes.toString());
	}

}
