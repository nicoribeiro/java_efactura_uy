package com.bluedot.efactura.pollers;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.reportes.ReportesDiarios;

import play.Configuration;
import play.Play;

/**
 * Reporte diario a DGI, una vez por dia despues de ReporteDiarioPoller.hora (reemplaza el cron con curl). Genera los
 * reportes faltantes (los que no estan aceptados, AR) de cantReportes dias a partir de hoy - diasDesde. Con los valores
 * por defecto (30 y 20) cubre de hace 30 a hace 11 dias: los ultimos 10 dias se reportan con atraso a proposito.
 *
 * Configuracion (application.conf):
 * <ul>
 * <li>ReporteDiarioPoller.run: true para activarlo (solo en una instancia)</li>
 * <li>ReporteDiarioPoller.hora: hora del dia (0-23) a partir de la cual corre, default 15</li>
 * <li>ReporteDiarioPoller.intervaloMinutos: cada cuanto revisa si ya es la hora, default 5</li>
 * <li>ReporteDiarioPoller.diasDesde: default 30</li>
 * <li>ReporteDiarioPoller.cantReportes: default 20</li>
 * <li>ReporteDiarioPoller.empresas: RUTs separados por coma; vacio = todas las empresas con firma digital</li>
 * </ul>
 */
public class ReporteDiarioPoller extends PollerRunner {


	static Logger logger = LoggerFactory.getLogger(ReporteDiarioPoller.class);

	/** Dia (yyyyMMdd) de la ultima ejecucion, para correr una sola vez por dia */
	private String ultimaEjecucion;

	public ReporteDiarioPoller() {
		super(logger, intervaloConfig("ReporteDiarioPoller", 5));
		// Desde el arranque, para que /procesos lo muestre antes de la primera ejecucion
		estado.setProgramacion(programacion(Play.application().configuration().getInt("ReporteDiarioPoller.hora", 15)));
	}

	private static String programacion(int hora) {
		long minutos = Play.application().configuration().getLong("ReporteDiarioPoller.intervaloMinutos", 5l);
		return String.format("diario desde las %02d:00 (revisa cada %d minutos)", hora, minutos);
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		Configuration config = Play.application().configuration();
		int hora = config.getInt("ReporteDiarioPoller.hora", 15);
		int diasDesde = config.getInt("ReporteDiarioPoller.diasDesde", 30);
		int cantReportes = config.getInt("ReporteDiarioPoller.cantReportes", 20);
		String rutsConfig = config.getString("ReporteDiarioPoller.empresas", "");

		estado.setProgramacion(programacion(hora));

		Calendar ahora = Calendar.getInstance();
		String hoy = new SimpleDateFormat("yyyyMMdd").format(ahora.getTime());
		if (hoy.equals(ultimaEjecucion) || ahora.get(Calendar.HOUR_OF_DAY) < hora)
			return;
		ultimaEjecucion = hoy;

		Calendar desde = Calendar.getInstance();
		desde.add(Calendar.DAY_OF_MONTH, -diasDesde);
		desde.set(Calendar.HOUR_OF_DAY, 0);
		desde.set(Calendar.MINUTE, 0);
		desde.set(Calendar.SECOND, 0);
		desde.set(Calendar.MILLISECOND, 0);
		String desdeTexto = new SimpleDateFormat("yyyy-MM-dd").format(desde.getTime());

		List<Empresa> empresas = empresas(rutsConfig);
		List<String> resumen = new ArrayList<String>();
		logger.info("Reporte diario: {} empresa(s), {} dias desde {} (solo faltantes)", empresas.size(), cantReportes, desdeTexto);

		for (Empresa empresa : empresas) {
			if (empresa.getFirmaDigital() == null || empresa.getFirmaDigital().getValidaHasta().before(new Date())) {
				logger.warn("Reporte diario: empresa {} sin firma digital vigente, se saltea", empresa.getRut());
				resumen.add(empresa.getRut() + ": sin firma digital vigente");
				continue;
			}
			try {
				JSONArray resultado = ReportesDiarios.generar(empresa, desde.getTime(), cantReportes, true, null);
				int generados = 0, errores = 0;
				for (int i = 0; i < resultado.length(); i++) {
					JSONObject r = resultado.getJSONObject(i);
					if (r.optInt("result_code", 0) == 0)
						generados++;
					else {
						errores++;
						logger.warn("Reporte diario: empresa {} dia {}: {} {}", empresa.getRut(), r.optString("fecha"),
								r.optString("result_message"), r.optString("result_detail"));
					}
				}
				logger.info("Reporte diario: empresa {}: {} generado(s), {} con error, {} ya aceptado(s)", empresa.getRut(),
						generados, errores, cantReportes - resultado.length());
				resumen.add(empresa.getRut() + ": " + generados + " generado(s), " + errores + " con error, "
						+ (cantReportes - resultado.length()) + " ya aceptado(s)");
			} catch (Throwable t) {
				logger.error("Reporte diario: error en empresa " + empresa.getRut(), t);
				resumen.add(empresa.getRut() + ": error " + t.getMessage());
			}
		}
		estado.setResumen("Días desde " + desdeTexto + " (" + cantReportes + "). "
				+ (resumen.isEmpty() ? "Sin empresas con firma digital" : String.join("; ", resumen)));
	}

	/** Las empresas de la configuracion, o todas las que tienen firma digital */
	private List<Empresa> empresas(String rutsConfig) {
		List<Empresa> empresas = new ArrayList<Empresa>();
		if (rutsConfig != null && !rutsConfig.trim().isEmpty()) {
			for (String rut : rutsConfig.split(",")) {
				Empresa empresa = Empresa.findByRUT(rut.trim());
				if (empresa != null)
					empresas.add(empresa);
				else
					logger.warn("Reporte diario: empresa {} de ReporteDiarioPoller.empresas no existe", rut.trim());
			}
			return empresas;
		}
		return Empresa.find(null, null, true, null, 1, 1000).item1;
	}
}
