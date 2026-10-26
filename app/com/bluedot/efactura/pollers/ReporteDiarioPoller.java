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

	public ReporteDiarioPoller(Configuration config) {
		super(logger, intervaloConfig(config, "ReporteDiarioPoller", 5));
		// Desde el arranque, para que /procesos lo muestre antes de la primera ejecucion
		estado.setProgramacion(programacion(config, config.getInt("ReporteDiarioPoller.hora", 15)));
	}

	private static String programacion(Configuration config, int hora) {
		long minutos = config.getLong("ReporteDiarioPoller.intervaloMinutos", 5l);
		return String.format("diario desde las %02d:00 (revisa cada %d minutos)", hora, minutos);
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		Configuration config = Play.application().configuration();
		int hora = config.getInt("ReporteDiarioPoller.hora", 15);
		int diasDesde = config.getInt("ReporteDiarioPoller.diasDesde", 30);
		int cantReportes = config.getInt("ReporteDiarioPoller.cantReportes", 20);
		String rutsConfig = config.getString("ReporteDiarioPoller.empresas", "");

		estado.setProgramacion(programacion(config, hora));

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
		JSONArray porEmpresa = new JSONArray();
		logger.info("Reporte diario: {} empresa(s), {} dias desde {} (solo faltantes)", empresas.size(), cantReportes, desdeTexto);

		for (Empresa empresa : empresas) {
			if (!firmaVigente(empresa, new Date())) {
				logger.warn("Reporte diario: empresa {} sin firma digital vigente, se saltea", empresa.getRut());
				resumen.add(empresa.getRut() + ": sin firma digital vigente");
				porEmpresa.put(new JSONObject().put("rut", empresa.getRut()).put("error", "sin firma digital vigente"));
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
				porEmpresa.put(new JSONObject().put("rut", empresa.getRut()).put("generados", generados).put("conError", errores)
						.put("yaAceptados", cantReportes - resultado.length()).put("reportes", resultado));
			} catch (Throwable t) {
				logger.error("Reporte diario: error en empresa " + empresa.getRut(), t);
				resumen.add(empresa.getRut() + ": error " + t.getMessage());
				porEmpresa.put(new JSONObject().put("rut", empresa.getRut()).put("error", String.valueOf(t.getMessage())));
			}
		}
		resultado("Días desde " + desdeTexto + " (" + cantReportes + "). "
				+ (resumen.isEmpty() ? "Sin empresas con firma digital" : String.join("; ", resumen)),
				new JSONObject().put("desde", desdeTexto).put("cantReportes", cantReportes).put("empresas", porEmpresa));
	}

	/** Las empresas de la configuracion, o todas las que tienen firma digital y no estan dadas de baja */
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
		// Sin las dadas de baja
		for (Empresa empresa : Empresa.find(null, null, true, null, 1, 1000).item1)
			if (empresa.isActiva())
				empresas.add(empresa);
		return empresas;
	}

	static boolean firmaVigente(Empresa empresa, Date ahora) {
		return empresa.getFirmaDigital() != null && empresa.getFirmaDigital().getValidaHasta() != null
				&& !empresa.getFirmaDigital().getValidaHasta().before(ahora);
	}

	/**
	 * Si el poller manda solo el reporte diario de esta empresa: esta activo (ReporteDiarioPoller.run), la empresa tiene
	 * firma digital vigente, y esta en ReporteDiarioPoller.empresas o, si esa lista esta vacia, no esta dada de baja. Lo
	 * mismo que hace executeConcreteAction.
	 */
	public static boolean cubre(Empresa empresa, Configuration config, Date ahora) {
		if (!config.getBoolean("ReporteDiarioPoller.run", false) || !firmaVigente(empresa, ahora))
			return false;
		String rutsConfig = config.getString("ReporteDiarioPoller.empresas", "");
		if (rutsConfig != null && !rutsConfig.trim().isEmpty()) {
			for (String rut : rutsConfig.split(","))
				if (rut.trim().equals(empresa.getRut()))
					return true;
			return false;
		}
		return empresa.isActiva();
	}

	/**
	 * Ultimo dia que el poller ya deberia haber reportado: cada dia, despues de la hora, cubre de hoy - diasDesde a hoy -
	 * diasDesde + cantReportes - 1 (con 30 y 20, hasta hace 11 dias); antes de la hora, lo de la corrida de ayer. Los dias
	 * posteriores todavia no se reportaron a proposito: no son faltantes.
	 */
	public static java.time.LocalDate hastaEsperado(Configuration config, java.time.LocalDateTime ahora) {
		return hastaEsperado(config.getInt("ReporteDiarioPoller.diasDesde", 30), config.getInt("ReporteDiarioPoller.cantReportes", 20),
				config.getInt("ReporteDiarioPoller.hora", 15), ahora);
	}

	static java.time.LocalDate hastaEsperado(int diasDesde, int cantReportes, int hora, java.time.LocalDateTime ahora) {
		// Como minimo hasta ayer: el reporte de un dia se manda cuando el dia termino
		int atraso = Math.max(diasDesde - cantReportes + 1, 1);
		if (ahora.getHour() < hora)
			atraso++;
		return ahora.toLocalDate().minusDays(atraso);
	}
}
