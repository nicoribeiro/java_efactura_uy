package com.bluedot.efactura.pollers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.empresas.EmisoresElectronicos;
import com.bluedot.efactura.model.Empresa;

import play.Configuration;
import play.Play;

/**
 * Sincroniza una vez por dia los emisores electronicos con DGI (ver EmisoresElectronicos): los que DGI informa como
 * nuevos o modificados en los ultimos diasAtras dias. Repasa varios dias para no perder cambios si un dia no corrio.
 *
 * Configuracion (application.conf):
 * <ul>
 * <li>EmisoresElectronicosPoller.run: true para activarlo (solo en una instancia)</li>
 * <li>EmisoresElectronicosPoller.hora: hora del dia (0-23) a partir de la cual corre, default 5</li>
 * <li>EmisoresElectronicosPoller.intervaloMinutos: cada cuanto revisa si ya es la hora, default 30</li>
 * <li>EmisoresElectronicosPoller.diasAtras: cuantos dias hacia atras pide los cambios, default 7</li>
 * <li>EmisoresElectronicosPoller.empresa: RUT de la empresa con cuya firma se consulta; vacio = la primera con firma
 * vigente</li>
 * </ul>
 */
public class EmisoresElectronicosPoller extends PollerRunner {

	static Logger logger = LoggerFactory.getLogger(EmisoresElectronicosPoller.class);

	/** Dia (yyyyMMdd) de la ultima ejecucion, para correr una sola vez por dia */
	private String ultimaEjecucion;

	public EmisoresElectronicosPoller(Configuration config) {
		super(logger, intervaloConfig(config, "EmisoresElectronicosPoller", 30));
		estado.setProgramacion(programacion(config.getInt("EmisoresElectronicosPoller.hora", 5)));
	}

	private static String programacion(int hora) {
		return String.format("diario desde las %02d:00", hora);
	}

	/** EmisoresElectronicos.ejecutar registra la ejecucion (tambien cuando se pide a mano) */
	@Override
	protected boolean registraSusEjecuciones() {
		return true;
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		Configuration config = Play.application().configuration();
		int hora = Math.max(0, Math.min(23, config.getInt("EmisoresElectronicosPoller.hora", 5)));
		int diasAtras = Math.max(1, config.getInt("EmisoresElectronicosPoller.diasAtras", 7));
		String rutFirmante = config.getString("EmisoresElectronicosPoller.empresa", "");
		estado.setProgramacion(programacion(hora));

		Calendar ahora = Calendar.getInstance();
		String hoy = new SimpleDateFormat("yyyyMMdd").format(ahora.getTime());
		if (hoy.equals(ultimaEjecucion) || ahora.get(Calendar.HOUR_OF_DAY) < hora)
			return;
		ultimaEjecucion = hoy;

		Empresa firmante = rutFirmante != null && !rutFirmante.trim().isEmpty() ? Empresa.findByRUT(rutFirmante.trim())
				: EmisoresElectronicos.firmantePorDefecto();

		Calendar desde = Calendar.getInstance();
		desde.add(Calendar.DAY_OF_MONTH, -diasAtras);
		JSONObject r = EmisoresElectronicos.ejecutar(desde.getTime(), firmante, "automático");
		estado.setResumen(r.getString("resumen"));
	}
}
