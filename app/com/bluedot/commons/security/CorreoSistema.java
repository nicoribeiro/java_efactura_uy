package com.bluedot.commons.security;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.MessagingHelper;

import play.Configuration;
import play.Play;

/**
 * Mails del sistema a los usuarios (codigos para confirmar el email y para recuperar la contrasena), con su propia
 * cuenta SMTP: mail.sistema.host, port, user, password y from. No usa la cuenta de ninguna empresa. Con
 * mail.sistema.log=true (o, si falta, mail.log=true) se loguean en lugar de mandarse.
 */
public class CorreoSistema {

	final static Logger logger = LoggerFactory.getLogger(CorreoSistema.class);

	/** Para mandar sin hacer esperar la respuesta (recuperar contrasena: que no se note si el email existe) */
	private static final ExecutorService ENVIOS = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "correo-sistema");
		t.setDaemon(true);
		return t;
	});

	public static void enviarCodigo(String para, CodigoEmail.Tipo tipo, String codigo) throws APIException {
		Configuration conf = Play.application().configuration();
		String host = conf.getString("mail.sistema.host", "");
		String from = conf.getString("mail.sistema.from", "");
		// Opcion propia (ej. en test se mandan los codigos pero no los mails de las empresas); si falta, la de mail.log
		boolean soloLog = conf.getBoolean("mail.sistema.log", conf.getBoolean("mail.log", false));
		if (!soloLog && (host.trim().isEmpty() || from.trim().isEmpty()))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("mail.sistema")
					.setDetailMessage("No está configurada la cuenta de mail del sistema (mail.sistema.host y from)");
		// Solo log (ej. test): sin from configurado igual tiene que ser una direccion valida para armar el mail
		if (from.trim().isEmpty())
			from = "efactura <no-responder@localhost>";

		boolean confirmar = tipo == CodigoEmail.Tipo.CONFIRMAR_EMAIL;
		String asunto = confirmar ? "Código para confirmar tu email" : "Código para recuperar tu contraseña";
		String cuerpo = "Hola,\n\n" + (confirmar ? "Tu código para confirmar tu email en efactura es: " : "Tu código para cambiar tu contraseña de efactura es: ")
				+ codigo + "\n\nVale " + CodigoEmail.VIGENCIA_MINUTOS + " minutos y se puede usar una sola vez.\n\n"
				+ "Si no lo pediste, ignorá este mail: tu cuenta sigue igual.\n\nefactura";

		boolean enviado = new MessagingHelper()
				.withCustomConfig(from, host, conf.getInt("mail.sistema.port", 465), conf.getString("mail.sistema.user", ""),
						conf.getString("mail.sistema.password", ""))
				.withSoloLog(soloLog)
				.sendEmail(para, cuerpo, asunto);
		if (!enviado)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("mail.sistema")
					.setDetailMessage("No se pudo enviar el mail con el código");
	}

	/** Lo manda en otro hilo; si falla solo queda en el log */
	public static void enviarCodigoSinEsperar(String para, CodigoEmail.Tipo tipo, String codigo) {
		ENVIOS.submit(() -> {
			try {
				enviarCodigo(para, tipo, codigo);
			} catch (Throwable t) {
				logger.error("No se pudo enviar el código (" + tipo + ") a " + para + ": " + t.getMessage());
			}
		});
	}
}
