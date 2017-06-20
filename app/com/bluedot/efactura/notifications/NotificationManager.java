package com.bluedot.efactura.notifications;

import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.commons.utils.DatabaseExecutor;
import com.bluedot.commons.utils.DatabaseExecutor.PromiseBlock;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.microControllers.implementation.CAEMicroControllerDefault;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import play.Play;

public class NotificationManager implements Runnable {

	private static final long INTERVAL = 1000L;

	final static Logger logger = LoggerFactory.getLogger(NotificationManager.class);

	private long sleepTimeInMillis;

	private long runs;

	public NotificationManager(long sleepTimeInMillis) {
		super();
		this.sleepTimeInMillis = sleepTimeInMillis;
		runs = 0;
	}

	public void run() {

		while (true) {
			try {
				if (INTERVAL * runs > sleepTimeInMillis) {
					runs = 0;
					try {
						DatabaseExecutor.syncDatabaseAction(new PromiseBlock<Void>() {
							public Void execute() {

								logger.info("Buscando Empresas ...");

								List<Empresa> empresas = Empresa.findAll();

								for (Empresa empresa : empresas) {
									if (empresa.getFirmaDigital() != null)
										checkFechaVencimientoFirma(empresa);
									if (empresa.getCAEs() != null && !empresa.getCAEs().isEmpty())
										try {
											checkCAEs(empresa);
										} catch (APIException e) {
											e.printStackTrace();
										}
								}
								
								logger.info("Finaliza Procesamiento de Empresas ...");

								return null;
							}

						}, true);

					} catch (Throwable e) {
						e.printStackTrace();
					}
				}
				runs++;
				Thread.sleep(INTERVAL);

			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	private void checkFechaVencimientoFirma(Empresa empresa) {

		logger.info("Chequeando Vencimiento Firma para Empresa: " + empresa.getNombreComercial());

		String subject = "Próximo Vencimiento de CAE";

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
		new MessagingHelper()
				.withCustomConfig(empresa.getFromEnvio(), empresa.getHostRecepcion(),
						Integer.parseInt(empresa.getPuertoRecepcion()), empresa.getUserRecepcion(),
						empresa.getPassRecepcion())
				.sendEmail(empresa.getMailNotificaciones(), body, null, subject, false);
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