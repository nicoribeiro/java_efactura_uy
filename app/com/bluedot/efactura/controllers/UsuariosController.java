package com.bluedot.efactura.controllers;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Credential;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Administracion de usuarios (solo ADMIN, siempre: /api/v1/usuarios esta entre las rutas de EmpresaAccessAction que
 * exigen ADMIN en cualquier modo). No hay borrado: se desactiva. Las contrasenas las genera la API (temporales, se
 * muestran una sola vez) y el usuario debe cambiarlas al ingresar. Cada cambio queda en el log "auditoria".
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
public class UsuariosController extends AbstractController {

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	/** Usuario resumido para el panel (sin tokens ni credenciales) */
	static JSONObject usuarioJson(User user) {
		JSONObject json = new JSONObject();
		json.put("id", user.getId());
		json.put("emailAddress", user.getEmailAddress());
		json.put("firstName", user.getFirstName() != null ? user.getFirstName() : JSONObject.NULL);
		json.put("lastName", user.getLastName() != null ? user.getLastName() : JSONObject.NULL);
		json.put("role", user.getRole().name());
		json.put("activo", user.isActivo());
		json.put("debeCambiarPassword", user.isDebeCambiarPassword());
		JSONArray empresas = new JSONArray();
		for (String rut : AccesoEmpresa.rutsPermitidos(user))
			empresas.put(rut);
		json.put("empresas", empresas);
		return json;
	}

	private User admin() throws APIException {
		return PermissionValidator.getSessionUser(ctx());
	}

	private void auditar(String accion, User usuario) throws APIException {
		auditoria.info(admin().getEmailAddress() + " " + accion + " usuario " + usuario.getEmailAddress() + " (id "
				+ usuario.getId() + ")");
	}

	private static String texto(JsonNode json, String campo) {
		JsonNode valor = json.get(campo);
		if (valor == null || valor.isNull())
			return null;
		String t = valor.asText().trim();
		return t.isEmpty() ? null : t;
	}

	private static Role rol(String valor) throws APIException {
		try {
			return Role.valueOf(valor);
		} catch (IllegalArgumentException | NullPointerException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("role", valor);
		}
	}

	private static void validarEmail(String email) throws APIException {
		if (email == null || !EMAIL.matcher(email).matches())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("emailAddress", email);
	}

	/** Cantidad de ADMIN activos (para no dejar el sistema sin administradores) */
	private static int adminsActivos() {
		int n = 0;
		for (User u : User.find(Role.ADMIN))
			if (u.isActivo())
				n++;
		return n;
	}

	/** El ultimo ADMIN activo no puede dejar de serlo ni desactivarse */
	private static void protegerUltimoAdmin(User user) throws APIException {
		if (user.getRole() == Role.ADMIN && user.isActivo() && adminsActivos() <= 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("usuario", user.getEmailAddress())
					.setDetailMessage("Es el único administrador activo");
	}

	private void noAUnoMismo(User user, String operacion) throws APIException {
		if (user.getId() == admin().getId())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("usuario", user.getEmailAddress())
					.setDetailMessage("No se puede " + operacion + " el propio usuario");
	}

	private static void darAccesoEmpresa(User user, String rut) throws APIException {
		Empresa.findByRUT(rut, true);
		String permisoId = AccesoEmpresa.permisoEmpresa(rut);
		if (!user.hasPermission(permisoId)) {
			Permission permiso = new Permission();
			permiso.setPermissionId(permisoId);
			permiso.setDescription("Acceso a la empresa " + rut);
			user.getPermissions().add(permiso);
		}
	}

	/**
	 * Crea un usuario con contrasena temporal (se devuelve una sola vez). Body: emailAddress, firstName, lastName, role
	 * (USER o ADMIN), empresas (RUTs, opcional).
	 */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> crear() throws APIException {
		JsonNode body = request().body().asJson();
		String email = texto(body, "emailAddress");
		validarEmail(email);
		if (User.findByEmailAddress(email) != null)
			throw APIException.raise(APIErrors.USER_ALREADY_EXISTS).withParams("emailAddress", email);
		String nombre = texto(body, "firstName");
		if (nombre == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("firstName");
		Role role = rol(texto(body, "role") != null ? texto(body, "role") : "USER");

		Set<String> ruts = new LinkedHashSet<String>();
		if (body.has("empresas") && body.get("empresas").isArray())
			for (JsonNode rut : body.get("empresas"))
				ruts.add(rut.asText().trim());

		String password = User.generarPasswordTemporal();
		User user = new User(email, password, nombre, texto(body, "lastName"));
		user.setRole(role);
		user.setActivo(true);
		user.setDebeCambiarPassword(true);
		for (String rut : ruts)
			darAccesoEmpresa(user, rut);
		user.save();
		auditar("creó (" + role + (ruts.isEmpty() ? "" : ", empresas " + ruts) + ")", user);

		JSONObject result = new JSONObject();
		result.put("usuario", usuarioJson(user));
		result.put("passwordTemporal", password);
		return json(result.toString());
	}

	public Promise<Result> ver(int uid) throws APIException {
		return json(usuarioJson(User.findById(uid, true)).toString());
	}

	/** Edita datos y rol. Body: emailAddress, firstName, lastName, role (los que vengan) */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> editar(int uid) throws APIException {
		User user = User.findById(uid, true);
		JsonNode body = request().body().asJson();
		StringBuilder cambios = new StringBuilder();

		String email = texto(body, "emailAddress");
		if (email != null && !email.equalsIgnoreCase(user.getEmailAddress())) {
			validarEmail(email);
			if (User.findByEmailAddress(email) != null)
				throw APIException.raise(APIErrors.USER_ALREADY_EXISTS).withParams("emailAddress", email);
			cambios.append(" email " + user.getEmailAddress() + " -> " + email);
			user.setEmailAddress(email);
		}
		if (body.has("firstName")) {
			String nombre = texto(body, "firstName");
			if (nombre == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("firstName");
			user.setFirstName(nombre);
		}
		if (body.has("lastName"))
			user.setLastName(texto(body, "lastName"));
		if (body.has("role")) {
			Role role = rol(texto(body, "role"));
			if (role != user.getRole()) {
				if (role != Role.ADMIN) {
					noAUnoMismo(user, "quitar el rol de administrador a");
					protegerUltimoAdmin(user);
				}
				cambios.append(" rol " + user.getRole() + " -> " + role);
				user.setRole(role);
			}
		}
		user.update();
		auditar("editó" + cambios, user);
		return json(usuarioJson(user).toString());
	}

	/** Genera una contrasena temporal nueva (se devuelve una sola vez) y cierra las sesiones del usuario */
	public Promise<Result> resetearPassword(int uid) throws APIException {
		User user = User.findById(uid, true);
		String password = User.generarPasswordTemporal();
		user.setPassword(password);
		user.setDebeCambiarPassword(true);
		user.update();
		int sesiones = user.cerrarSesiones();
		auditar("generó contraseña temporal (" + sesiones + " sesión(es) cerrada(s)) para", user);

		JSONObject result = new JSONObject();
		result.put("usuario", usuarioJson(user));
		result.put("passwordTemporal", password);
		return json(result.toString());
	}

	/** Activa o desactiva. Body: activo (boolean). Al desactivar cierra sus sesiones */
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> cambiarActivo(int uid) throws APIException {
		User user = User.findById(uid, true);
		JsonNode body = request().body().asJson();
		if (!body.has("activo"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("activo");
		boolean activo = body.get("activo").asBoolean();
		if (!activo && user.isActivo()) {
			noAUnoMismo(user, "desactivar");
			protegerUltimoAdmin(user);
		}
		user.setActivo(activo);
		user.update();
		int sesiones = activo ? 0 : user.cerrarSesiones();
		auditar((activo ? "activó" : "desactivó (" + sesiones + " sesión(es) cerrada(s))"), user);
		return json(usuarioJson(user).toString());
	}

	/** Cierra (invalida) todas las sesiones del usuario */
	public Promise<Result> cerrarSesiones(int uid) throws APIException {
		User user = User.findById(uid, true);
		int sesiones = user.cerrarSesiones();
		auditar("cerró " + sesiones + " sesión(es) de", user);
		JSONObject result = new JSONObject();
		result.put("cerradas", sesiones);
		return json(result.toString());
	}

	/** Credencial HMAC: solo la key y si es valida; el secret no se vuelve a mostrar */
	public Promise<Result> verCredenciales(int uid) throws APIException {
		User user = User.findById(uid, true);
		Credential c = user.getCredentials();
		JSONObject result = new JSONObject();
		result.put("key", c != null ? c.getToken() : JSONObject.NULL);
		result.put("valida", c != null && c.isValid());
		return json(result.toString());
	}

	/** Regenera la credencial HMAC (la anterior deja de funcionar). Devuelve key y secret una sola vez */
	public Promise<Result> regenerarCredenciales(int uid) throws APIException {
		User user = User.findById(uid, true);
		if (user.getCredentials() != null)
			user.getCredentials().revoke();
		user.setCredentials(Credential.generateCredential());
		user.update();
		auditar("regeneró las credenciales HMAC de", user);
		JSONObject result = new JSONObject();
		result.put("key", user.getCredentials().getToken());
		result.put("secret", user.getCredentials().getSecret());
		return json(result.toString());
	}
}
