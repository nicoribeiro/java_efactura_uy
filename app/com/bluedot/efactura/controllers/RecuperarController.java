package com.bluedot.efactura.controllers;

import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.CodigoEmail;
import com.bluedot.commons.security.CorreoSistema;
import com.bluedot.commons.security.LimiteIntentosLogin;
import com.bluedot.commons.security.Session;
import com.bluedot.commons.security.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;

/**
 * Recuperar la contrasena sin sesion, con un codigo que se manda por mail (ver CodigoEmail). Solo para usuarios activos
 * con el email confirmado. Para no revelar que emails tienen cuenta, pedir el codigo responde siempre lo mismo (y el mail
 * se manda en otro hilo, asi tarda igual). Cada pedido y cada codigo incorrecto cuentan para el limite por IP de
 * LimiteIntentosLogin.
 */
@ErrorMessage
@Tx
public class RecuperarController extends AbstractController {

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	/** Largo minimo de una contrasena nueva (el mismo que al cambiarla) */
	private static final int MINIMO_PASSWORD = 10;

	/** Pide un codigo. Body: emailAddress. Responde siempre ok */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> pedirCodigo() throws APIException {
		String email = texto(request().body().asJson(), "emailAddress");
		String ip = LimiteIntentosLogin.ipCliente(request());
		limitar(ip);
		// Cada pedido cuenta para el limite de la IP (no para el usuario: no se le bloquea el login a nadie)
		LimiteIntentosLogin.instancia().registrarFalla(null, ip, System.currentTimeMillis());

		User user = email != null ? User.findByEmailAddress(email) : null;
		if (user != null && user.isActivo() && user.isValidated()) {
			try {
				String codigo = CodigoEmail.emitir(user, CodigoEmail.Tipo.RECUPERAR_PASSWORD, new Date());
				CorreoSistema.enviarCodigoSinEsperar(user.getEmailAddress(), CodigoEmail.Tipo.RECUPERAR_PASSWORD, codigo);
				auditoria.info(user.getEmailAddress() + " pidió un código para recuperar su contraseña desde " + ip);
			} catch (APIException e) {
				// Pidio otro hace menos de un minuto o muchos en la hora: no se manda, pero se responde igual
				auditoria.info(user.getEmailAddress() + " pidió un código para recuperar su contraseña desde " + ip + " (no enviado: "
						+ e.getMessage() + ")");
			}
		}
		return json(new JSONObject().put("ok", true).put("vigenciaMinutos", CodigoEmail.VIGENCIA_MINUTOS).toString());
	}

	/** Cambia la contrasena con el codigo. Body: emailAddress, codigo, nueva. Cierra todas las sesiones del usuario */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> cambiarPassword() throws APIException {
		JsonNode body = request().body().asJson();
		String email = texto(body, "emailAddress");
		String codigo = texto(body, "codigo");
		String nueva = body != null && body.has("nueva") ? body.get("nueva").asText() : null;
		String ip = LimiteIntentosLogin.ipCliente(request());
		limitar(ip);

		if (nueva == null || nueva.length() < MINIMO_PASSWORD)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("nueva")
					.setDetailMessage("La contraseña nueva debe tener al menos " + MINIMO_PASSWORD + " caracteres");

		User user = email != null ? User.findByEmailAddress(email) : null;
		try {
			if (user == null || !user.isActivo() || !user.isValidated())
				throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("El código venció o no existe: pedí uno nuevo");
			CodigoEmail.verificar(user, CodigoEmail.Tipo.RECUPERAR_PASSWORD, codigo, new Date());
		} catch (APIException e) {
			// Solo para la IP: el codigo ya admite pocos intentos, y bloquear al usuario le impediria recuperar la contrasena
			LimiteIntentosLogin.instancia().registrarFalla(null, ip, System.currentTimeMillis());
			throw e;
		}

		user.setPassword(nueva);
		user.setDebeCambiarPassword(false);
		user.update();
		int cerradas = 0;
		for (Session session : Session.findByUser(user))
			if (Boolean.TRUE.equals(session.isValid())) {
				session.setValid(false);
				session.update();
				cerradas++;
			}
		LimiteIntentosLogin.instancia().registrarExito(email);
		auditoria.info(user.getEmailAddress() + " recuperó su contraseña con un código desde " + ip + " (" + cerradas + " sesión(es) cerrada(s))");
		return json(new JSONObject().put("ok", true).toString());
	}

	/** Con el limite de intentos en enforce, rechaza si la IP esta bloqueada */
	private static void limitar(String ip) throws APIException {
		if (LimiteIntentosLogin.modo() == LimiteIntentosLogin.Modo.OFF)
			return;
		int minutos = LimiteIntentosLogin.instancia().minutosBloqueado(null, ip);
		if (minutos > 0 && LimiteIntentosLogin.modo() == LimiteIntentosLogin.Modo.ENFORCE)
			throw APIException.raise(APIErrors.DEMASIADOS_INTENTOS).withParams(minutos);
	}

	private static String texto(JsonNode body, String campo) {
		if (body == null || !body.has(campo) || body.get(campo).isNull())
			return null;
		String valor = body.get(campo).asText().trim();
		return valor.isEmpty() ? null : valor;
	}
}
