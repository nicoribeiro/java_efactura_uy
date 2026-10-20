package com.bluedot.efactura.controllers;

import com.bluedot.efactura.commons.Commons;

import com.bluedot.efactura.security.AccesoEmpresa;
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
import com.bluedot.commons.security.PermissionValidator;
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

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");
	
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
	
	@Inject
	public ReportController(PollerManager pollerManager){
		super();
	}
	
	@ApiOperation(value = "Generar Reporte Diario",
		    response = ReporteDiario.class
		    )
	public Promise<Result> generarReporteDiario(String rut, String fecha, int cantReportes, int soloFaltantes) throws APIException {

		// Envia reportes a DGI: solo ADMIN
		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "POST /empresa/" + rut + "/reporteDiario", true);

		Empresa empresa = Empresa.findByRUT(rut, true);
		Date date = DateHandler.fromStringToDate(fecha, sdf);

		/*
		 * Body (opcional): Output es la carpeta donde guardar el XML de cada reporte (dentro de seguridad.salidas.path)
		 */
		JsonNode jsonNode = request().body().asJson();
		JSONObject document = jsonNode != null ? new JSONObject(jsonNode.toString()) : new JSONObject();
		String output = document.has("Output") ? Commons.carpetaSalida(document.getString("Output")) : null;

		JSONArray reportes = ReportesDiarios.generar(empresa, date, cantReportes, soloFaltantes > 0, output);

		int generados = 0;
		for (int i = 0; i < reportes.length(); i++)
			if (reportes.getJSONObject(i).optInt("result_code", 0) == 0)
				generados++;
		auditoria.info(PermissionValidator.getSessionUser(ctx()).getEmailAddress() + " generó el reporte diario de la empresa "
				+ rut + " desde " + fecha + " (" + cantReportes + " día(s)" + (soloFaltantes > 0 ? ", solo faltantes" : "")
				+ "): " + generados + " generado(s), " + (reportes.length() - generados) + " con error");

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
