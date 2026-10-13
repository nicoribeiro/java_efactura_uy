package com.bluedot.efactura.pollers;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.SobreRecibido;
import com.bluedot.efactura.recibidos.RespuestaCfes;

import play.Configuration;
import play.Play;

/**
 * Envia la respuesta a los CFE (ME_) de los sobres recibidos cuando se cumple el plazo de la empresa receptora
 * (Empresa.diasRespuestaCfe o RespuestaCFE.diasPorDefecto). Ver RespuestaCfes.
 *
 * Configuracion (application.conf):
 * <ul>
 * <li>RespuestaCfePoller.run: true para activarlo (solo en una instancia)</li>
 * <li>RespuestaCfePoller.intervaloMinutos: cada cuanto revisa, default 30</li>
 * <li>RespuestaCfePoller.maximo: cuantos sobres pendientes revisa por vez, default 500</li>
 * </ul>
 */
public class RespuestaCfePoller extends PollerRunner {

	static Logger logger = LoggerFactory.getLogger(RespuestaCfePoller.class);

	public RespuestaCfePoller(Configuration config) {
		super(logger, intervaloConfig(config, "RespuestaCfePoller", 30));
		estado.setProgramacion("cada " + config.getLong("RespuestaCfePoller.intervaloMinutos", 30l) + " minutos");
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		int maximo = Play.application().configuration().getInt("RespuestaCfePoller.maximo", 500);
		Date ahora = new Date();
		int pendientes = 0, enviadas = 0, errores = 0;
		List<String> fallidos = new ArrayList<String>();
		for (SobreRecibido sobre : SobreRecibido.findRespuestaCfePendiente(Math.max(1, maximo))) {
			pendientes++;
			Date programada = RespuestaCfes.programada(sobre);
			if (programada == null || programada.after(ahora))
				continue;
			try {
				RespuestaCfes.enviar(sobre, "automático");
				enviadas++;
			} catch (Throwable t) {
				errores++;
				fallidos.add(sobre.getNombreArchivo() + ": " + t.getMessage());
				logger.warn("Respuesta a CFE: no se pudo enviar la del sobre " + sobre.getNombreArchivo() + ": " + t.getMessage());
			}
		}
		estado.setResumen(pendientes + " sobre(s) pendiente(s), " + enviadas + " respuesta(s) enviada(s)"
				+ (errores > 0 ? ", " + errores + " con error (" + String.join("; ", fallidos.subList(0, Math.min(3, fallidos.size()))) + ")" : ""));
	}
}
