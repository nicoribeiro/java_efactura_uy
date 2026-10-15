package com.bluedot.efactura.controllers;

import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.Session;
import com.bluedot.commons.security.CodigoEmail;
import com.bluedot.commons.security.CorreoSistema;
import com.bluedot.commons.security.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Cuenta del usuario logueado (cualquier rol): su perfil y el cambio de su contrasena. Es lo unico que se puede usar
 * con una contrasena temporal (ver Secured).
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class CuentaController extends AbstractController {

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	/** Largo minimo de una contrasena nueva */
	private static final int MINIMO_PASSWORD = 10;

	public Promise<Result> ver() throws APIException {
		return json(UsuariosController.usuarioJson(PermissionValidator.getSessionUser(ctx())).toString());
	}

	/** Manda un codigo al email propio para confirmarlo (ver CodigoEmail). Un codigo por minuto */
	public Promise<Result> enviarCodigoEmail() throws APIException {
		User user = PermissionValidator.getSessionUser(ctx());
		if (user.isValidated())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("email", user.getEmailAddress())
					.setDetailMessage("Tu email ya está confirmado");
		String codigo = CodigoEmail.emitir(user, CodigoEmail.Tipo.CONFIRMAR_EMAIL, new Date());
		CorreoSistema.enviarCodigo(user.getEmailAddress(), CodigoEmail.Tipo.CONFIRMAR_EMAIL, codigo);
		auditoria.info(user.getEmailAddress() + " pidió un código para confirmar su email");
		return json(new JSONObject().put("enviado", true).put("email", user.getEmailAddress())
				.put("vigenciaMinutos", CodigoEmail.VIGENCIA_MINUTOS).toString());
	}

	/** Confirma el email propio con el codigo recibido. Body: codigo */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> confirmarEmail() throws APIException {
		User user = PermissionValidator.getSessionUser(ctx());
		JsonNode body = request().body().asJson();
		String codigo = body != null && body.has("codigo") ? body.get("codigo").asText() : null;
		CodigoEmail.verificar(user, CodigoEmail.Tipo.CONFIRMAR_EMAIL, codigo, new Date());
		user.confirmarEmail();
		user.update();
		auditoria.info(user.getEmailAddress() + " confirmó su email");
		return json(UsuariosController.usuarioJson(user).toString());
	}

	/** Cambia la contrasena propia. Body: actual, nueva. Cierra las demas sesiones del usuario */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> cambiarPassword() throws APIException {
		User user = PermissionValidator.getSessionUser(ctx());
		JsonNode body = request().body().asJson();
		String actual = body.has("actual") ? body.get("actual").asText() : null;
		String nueva = body.has("nueva") ? body.get("nueva").asText() : null;

		// Para cambiar la contrasena el email tiene que estar confirmado (es por donde se recupera)
		if (!user.isValidated())
			throw APIException.raise(APIErrors.EMAIL_SIN_CONFIRMAR);
		if (actual == null || User.findByEmailAddressAndPassword(user.getEmailAddress(), actual) == null)
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("La contraseña actual no es correcta");
		if (nueva == null || nueva.length() < MINIMO_PASSWORD)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("nueva")
					.setDetailMessage("La contraseña nueva debe tener al menos " + MINIMO_PASSWORD + " caracteres");
		if (nueva.equals(actual))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("nueva")
					.setDetailMessage("La contraseña nueva debe ser distinta de la actual");

		user.setPassword(nueva);
		user.setDebeCambiarPassword(false);
		user.update();

		// Cierra las demas sesiones; la actual sigue valida
		String tokenActual = request().getHeader(Secured.AUTH_HEADER);
		int cerradas = 0;
		for (Session session : Session.findByUser(user))
			if (Boolean.TRUE.equals(session.isValid()) && !session.getToken().equals(tokenActual)) {
				session.setValid(false);
				session.update();
				cerradas++;
			}
		auditoria.info(user.getEmailAddress() + " cambió su contraseña (" + cerradas + " otra(s) sesión(es) cerrada(s))");

		return json(UsuariosController.usuarioJson(user).toString());
	}
}
