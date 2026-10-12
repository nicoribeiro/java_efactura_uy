package com.bluedot.efactura.reportes;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;

import dgi.classes.respuestas.reporte.EstadoACKRepType;

/**
 * Generacion de reportes diarios a DGI para una serie de dias. La usan el endpoint POST
 * /empresa/:rut/reporteDiario/:fecha y el ReporteDiarioPoller.
 */
public class ReportesDiarios {

	final static Logger logger = LoggerFactory.getLogger(ReportesDiarios.class);

	/**
	 * Un lock por empresa para que el poller y el endpoint no generen reportes de la misma empresa a la vez (podrian
	 * repetir el secuencial de un dia). Es local a la JVM, como el poller.
	 */
	private static final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

	/**
	 * Genera los reportes de cantReportes dias a partir de desde (inclusive). Con soloFaltantes se saltean los dias cuyo
	 * reporte mas reciente ya esta aceptado por DGI (AR). Un error en un dia no corta el resto: queda en el resultado de
	 * ese dia.
	 *
	 * @param output
	 *            carpeta donde guardar el XML de cada reporte generado; null = no se guarda
	 * @return un elemento por dia procesado (los salteados por AR no aparecen), con el reporte y result_code
	 */
	public static JSONArray generar(Empresa empresa, Date desde, int cantReportes, boolean soloFaltantes, String output)
			throws APIException {

		if (cantReportes < 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("cantReportes", cantReportes);

		if (empresa.getFirmaDigital() == null || empresa.getFirmaDigital().getValidaHasta().before(new Date()))
			throw APIException.raise(APIErrors.FIRMA_DIGITAL_VENCIDA);

		ReentrantLock lock = locks.computeIfAbsent(empresa.getRut(), rut -> new ReentrantLock());
		if (!lock.tryLock())
			throw APIException.raise(APIErrors.REPORTE_DIARIO_EN_CURSO).withParams(empresa.getRut());
		try {
			return generarConLock(empresa, desde, cantReportes, soloFaltantes, output);
		} finally {
			lock.unlock();
		}
	}

	private static JSONArray generarConLock(Empresa empresa, Date desde, int cantReportes, boolean soloFaltantes,
			String output) throws APIException {

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd");

		JSONArray reportes = new JSONArray();

		for (int i = 0; i < cantReportes; i++) {
			Date dia = DateHandler.add(desde, i, Calendar.DAY_OF_MONTH);
			JSONObject error = null;
			ReporteDiario reporte = null;
			try {
				if (soloFaltantes && aceptado(empresa, dia))
					continue;

				logger.info("Generando reporte diario empresa:{} dia:{}", empresa.getRut(), sdf.format(dia));

				reporte = factory.getServiceMicroController(empresa).generarReporteDiario(dia);

				if (output != null)
					IO.writeFile(output + "/reporte_" + reporte.getId() + ".xml", reporte.getXml());
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			} catch (IOException e) {
				throw APIException.raise(e);
			}

			JSONObject reporteJSON = new JSONObject();
			if (reporte != null)
				reporteJSON = EfacturaJSONSerializerProvider.getReporteDiarioSerializer().objectToJson(reporte);
			else
				reporteJSON.put("fecha", iso.format(dia));

			reportes.put(JSONUtils.merge(reporteJSON, error != null ? error : new JSONObject(AbstractController.OK)));
		}

		return reportes;
	}

	/** El reporte mas reciente del dia (el que tiene estado y mayor secuencial) esta aceptado por DGI */
	private static boolean aceptado(Empresa empresa, Date dia) {
		List<ReporteDiario> reportesList = ReporteDiario.findByEmpresaFecha(empresa, dia);
		if (reportesList == null || reportesList.isEmpty())
			return false;

		ReporteDiario reporteDiario = reportesList.get(0);
		for (ReporteDiario rd : reportesList)
			if ((reporteDiario.getEstado() == null && rd.getEstado() != null) || (rd.getSecuencial() > reporteDiario.getSecuencial()))
				reporteDiario = rd;

		return reporteDiario.getEstado() == EstadoACKRepType.AR;
	}
}
