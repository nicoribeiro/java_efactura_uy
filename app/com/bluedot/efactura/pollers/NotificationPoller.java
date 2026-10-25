package com.bluedot.efactura.pollers;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.MessagingHelper;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.microControllers.implementation.CAEMicroControllerDefault;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import play.Play;

public class NotificationPoller extends PollerRunner {


	final static Logger logger = LoggerFactory.getLogger(NotificationPoller.class);


	/** Avisos enviados en la ejecucion en curso */
	private org.json.JSONArray avisos = new org.json.JSONArray();

	public NotificationPoller(play.Configuration config) {
		super(logger, intervaloConfig(config, "NotificationPoller", 60 * 24));
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		
		logger.info("Buscando Empresas ...");

		// Solo las empresas que emiten con el sistema (con firma digital o CAEs) y no estan dadas de baja
		List<Empresa> empresas = Empresa.findConFirmaDigitalOCAEs();

		logger.info(empresas.size() + " empresas con firma digital o CAEs");

		avisos = new org.json.JSONArray();
		int errores = 0;
		for (Empresa empresa : empresas) {
			// Un error en una empresa (ej: configuracion de mail incompleta) no debe impedir notificar a las demas
			try {
				if (empresa.getFirmaDigital() != null)
					checkFechaVencimientoFirma(empresa);
			} catch (IllegalStateException e) {
				logger.error(e.getMessage());
				errores++;
			} catch (RuntimeException e) {
				logger.error("Error chequeando vencimiento de firma de la empresa " + empresa.getRut(), e);
				errores++;
			}
			try {
				if (empresa.getCaes() != null && !empresa.getCaes().isEmpty())
					checkCAEs(empresa);
			} catch (IllegalStateException e) {
				logger.error(e.getMessage());
				errores++;
			} catch (APIException | RuntimeException e) {
				logger.error("Error chequeando CAEs de la empresa " + empresa.getRut(), e);
				errores++;
			}
		}

		logger.info("Finaliza Procesamiento de Empresas ...");
		resultado(empresas.size() + " empresa(s) revisada(s), " + avisos.length() + " aviso(s) enviado(s)"
				+ (errores > 0 ? ", " + errores + " error(es) (ver el log)" : ""),
				new org.json.JSONObject().put("empresas", empresas.size()).put("avisos", avisos).put("errores", errores));
		
	}

	private void checkFechaVencimientoFirma(Empresa empresa) {

		logger.info("Chequeando Vencimiento Firma para Empresa: " + empresa.getNombreComercial());

		String subject = "Próximo Vencimiento de Firma Digital";

		String cabezal = Play.application().configuration().getString("mail.notificaciones.cabezal").replace("<nl>",
				"\n");

		String firma = Play.application().configuration().getString("mail.notificaciones.firma").replace("<nl>",
				"\n");
		
		if (empresa.getFirmaDigital().getValidaHasta() != null && DateHandler.diff(new Date(), empresa.getFirmaDigital().getValidaHasta()) < Play
				.application().configuration().getInt("notificaciones.firma.vencimiento", 30)) {
			String body = cabezal + "La Firma digital de la empresa " + empresa.getNombreComercial()
					+ " vencera el proximo " + empresa.getFirmaDigital().getValidaHasta() + "\n"
					+ "Por favor asegurese de renovarla antes de la fecha de vencimiento de forma de evitar cortes en la emision de documentos electrónicos."
					+ firma;

			sendMail(empresa, subject, body);

		}
		
	}

	/**
	 * @param empresa
	 * @param subject
	 * @param body
	 */
	private void sendMail(Empresa empresa, String subject, String body) {
		String falta = faltaConfiguracionCorreo(empresa);
		if (falta != null)
			throw new IllegalStateException("No se puede avisar \"" + subject + "\" a la empresa " + empresa.getRut()
					+ ": falta configuración de correo (" + falta + ")");
		boolean enviado = new MessagingHelper()
				.withCustomConfig(empresa.getFromEnvio(), empresa.getHostRecepcion(),
						Integer.parseInt(empresa.getPuertoRecepcion()), empresa.getUserRecepcion(),
						empresa.getPassRecepcion())
				.sendEmail(empresa.getMailNotificaciones(), body, null, subject, false);
		// Solo cuenta como aviso enviado si el mail salio (sendEmail deja el detalle del error en el log)
		if (!enviado)
			throw new IllegalStateException("No se pudo enviar el aviso \"" + subject + "\" a la empresa " + empresa.getRut());
		avisos.put(new org.json.JSONObject().put("rut", empresa.getRut()).put("asunto", subject).put("para", String.valueOf(empresa.getMailNotificaciones())));
	}

	/** Los datos de correo que le faltan a la empresa para mandar un aviso, o null si estan todos */
	static String faltaConfiguracionCorreo(Empresa empresa) {
		java.util.List<String> faltan = new java.util.ArrayList<String>();
		if (vacio(empresa.getHostRecepcion()))
			faltan.add("host");
		if (vacio(empresa.getPuertoRecepcion()))
			faltan.add("puerto");
		else
			try {
				Integer.parseInt(empresa.getPuertoRecepcion().trim());
			} catch (NumberFormatException e) {
				faltan.add("puerto válido");
			}
		if (vacio(empresa.getUserRecepcion()))
			faltan.add("usuario");
		if (vacio(empresa.getPassRecepcion()))
			faltan.add("contraseña");
		if (vacio(empresa.getFromEnvio()))
			faltan.add("remitente");
		if (vacio(empresa.getMailNotificaciones()))
			faltan.add("mail de notificaciones");
		return faltan.isEmpty() ? null : String.join(", ", faltan);
	}

	private static boolean vacio(String s) {
		return s == null || s.trim().isEmpty();
	}

	private void checkCAEs(Empresa empresa) throws APIException {

		logger.info("Chequeando CAEs para Empresa: " + empresa.getNombreComercial());

		CAEMicroControllerDefault caeMicroControllerDefault = new CAEMicroControllerDefault(empresa);

		

		String cabezal = Play.application().configuration().getString("mail.notificaciones.cabezal")
				.replace("<nl>", "\n");

		String firma = Play.application().configuration().getString("mail.notificaciones.firma").replace("<nl>",
				"\n");
		
		for (TipoDoc tipoDoc : caeMicroControllerDefault.getTipoDoc()) {
			if (DateHandler.diff(new Date(), caeMicroControllerDefault.getCAE(tipoDoc).getFechaVencimiento()) < Play.application().configuration()
					.getInt("notificaciones.cae.vencimiento", 7) && caeMicroControllerDefault.getCantCAEValidos(tipoDoc) == 1) {
				
				String subject = "Próximo Vencimiento de CAE";
				String body = cabezal + "El CAE de tipo " + tipoDoc.friendlyName
						+ " vencera el proximo " + caeMicroControllerDefault.getCAE(tipoDoc).getFechaVencimiento() + "\n"
						+ "Por favor asegurese de que exista otro CAE del mismo tipo antes de la fecha de vencimiento."
						+ firma;

				sendMail(empresa, subject, body);

			}
			
			if ( caeMicroControllerDefault.getPorcentajeUsoCAE(tipoDoc) > Play.application().configuration().getInt("notificaciones.cae.porcentajeUsoCritico", 90)) {
				
				String subject = "Uso de numeros CAE";
				String body = cabezal + "El uso de la numeracion para los documentos de tipo " + tipoDoc.friendlyName
						+ " ha alcanzado un uso del " + caeMicroControllerDefault.getPorcentajeUsoCAE(tipoDoc) + "%. \n"
						+ "Por favor asegurese de solicitar un CAE de este tipo a la brevedad de forma de evitar cortes en la emision de documentos electrónicos."
						+ firma;

				sendMail(empresa, subject, body);

			}
		}
	}

}