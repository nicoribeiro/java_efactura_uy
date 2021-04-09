package com.bluedot.efactura.controllers;

import java.io.IOException;
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
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.pollers.PollerManager;
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
	public Promise<Result> generarReporteDiario(String rut, String fecha, int cantReportes) throws APIException {

		if (cantReportes < 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("cantReportes", cantReportes);

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		Date date = DateHandler.fromStringToDate(fecha, sdf);

		JSONArray reportes = new JSONArray();

		/*
		 * Body
		 */
		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());
		
		for (int i = 0; i < cantReportes; i++) {
			JSONObject error = null;
			ReporteDiario reporte = null;
			try {

				logger.info("Generando reporte diario empresa:{} dia:{}", empresa.getRut(), sdf.format(DateHandler.add(date, i, Calendar.DAY_OF_MONTH)));
				
				reporte = factory.getServiceMicroController(empresa)
						.generarReporteDiario(DateHandler.add(date, i, Calendar.DAY_OF_MONTH));
				
				if (document.has("Output")) {
					String filepath = document.getString("Output");
					IO.writeFile(filepath + "/reporte_" + reporte.getId() + ".xml", reporte.getXml());
				}
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			} catch (IOException e) {
				throw APIException.raise(e);
			}

			JSONObject reporteJSON = new JSONObject();

			if (reporte != null)
				reporteJSON = EfacturaJSONSerializerProvider.getReporteDiarioSerializer().objectToJson(reporte);
			else {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				reporteJSON.put("fecha", sdf.format(DateHandler.add(date, i, Calendar.DAY_OF_MONTH)));
			}
				

			if (error != null)
				reportes.put(JSONUtils.merge(reporteJSON, error));
			else
				reportes.put(JSONUtils.merge(reporteJSON, new JSONObject(OK)));

		}

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
