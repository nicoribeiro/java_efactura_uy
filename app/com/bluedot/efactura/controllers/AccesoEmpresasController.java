package com.bluedot.efactura.controllers;

import java.util.List;

import com.bluedot.commons.security.ExpiracionSesion;

import com.bluedot.commons.security.Session;

import java.util.Date;

import java.text.SimpleDateFormat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Asignacion de empresas a usuarios (permiso access_empresa_{rut}) y usuarios de una empresa. Solo ADMIN, siempre:
 * lo exige {@link EmpresaAccess} para /api/v1/users/{uid}/empresas y /api/v1/empresa/{rut}/usuarios sin importar
 * seguridad.accesoEmpresa.
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
public class AccesoEmpresasController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(AccesoEmpresasController.class);

	/**
	 * Empresas a las que el usuario tiene acceso. Un ADMIN accede a todas, aunque no tenga permisos explicitos.
	 */
	public Promise<Result> getEmpresasUsuario(int uid) throws APIException {
		User user = User.findById(uid, true);

		JSONArray empresas = new JSONArray();
		for (String rut : AccesoEmpresa.rutsPermitidos(user)) {
			Empresa empresa = Empresa.findByRUT(rut);
			JSONObject json = new JSONObject();
			json.put("rut", rut);
			json.put("razon", empresa != null && empresa.getRazon() != null ? empresa.getRazon() : JSONObject.NULL);
			empresas.put(json);
		}

		JSONObject result = new JSONObject();
		result.put("userId", user.getId());
		result.put("emailAddress", user.getEmailAddress());
		result.put("role", user.getRole().name());
		result.put("accesoTotal", AccesoEmpresa.esAdmin(user));
		result.put("empresas", empresas);

		return json(result.toString());
	}

	/**
	 * Todos los usuarios con su rol, empresas y sesiones, y los modos de seguridad vigentes. Solo ADMIN, siempre. No
	 * devuelve tokens ni credenciales.
	 */
	public Promise<Result> getUsuarios() throws APIException {
		Date ahora = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

		JSONArray usuarios = new JSONArray();
		for (User user : User.findAll()) {
			JSONObject json = usuarioJson(user, AccesoEmpresa.esAdmin(user) ? "ADMIN" : "PERMISO");
			json.put("creado", user.getCreationDate() != null ? sdf.format(user.getCreationDate()) : JSONObject.NULL);

			JSONArray empresas = new JSONArray();
			for (String rut : AccesoEmpresa.rutsPermitidos(user)) {
				Empresa empresa = Empresa.findByRUT(rut);
				JSONObject e = new JSONObject();
				e.put("rut", rut);
				e.put("razon", empresa != null && empresa.getRazon() != null ? empresa.getRazon() : JSONObject.NULL);
				empresas.put(e);
			}
			json.put("accesoTotal", AccesoEmpresa.esAdmin(user));
			json.put("empresas", empresas);

			// Sesiones: las validas se clasifican segun las reglas de expiracion (aunque el modo no sea enforce)
			int total = 0, vigentes = 0, vencidas = 0;
			Session ultima = null;
			for (Session session : Session.findByUser(user)) {
				total++;
				if (!Boolean.TRUE.equals(session.isValid()))
					continue;
				if (ExpiracionSesion.motivoVencimiento(session, ahora) == null)
					vigentes++;
				else
					vencidas++;
				Date uso = session.getLastAccess() != null ? session.getLastAccess() : session.getCreationTimestamp();
				Date usoUltima = ultima == null ? null
						: (ultima.getLastAccess() != null ? ultima.getLastAccess() : ultima.getCreationTimestamp());
				if (ultima == null || (uso != null && (usoUltima == null || uso.after(usoUltima))))
					ultima = session;
			}
			JSONObject sesiones = new JSONObject();
			sesiones.put("total", total);
			sesiones.put("vigentes", vigentes);
			sesiones.put("vencidas", vencidas);
			if (ultima != null) {
				JSONObject u = new JSONObject();
				u.put("creada", ultima.getCreationTimestamp() != null ? sdf.format(ultima.getCreationTimestamp()) : JSONObject.NULL);
				u.put("ultimoAcceso", ultima.getLastAccess() != null ? sdf.format(ultima.getLastAccess()) : JSONObject.NULL);
				u.put("ip", ultima.getIp() != null ? ultima.getIp() : JSONObject.NULL);
				u.put("userAgent", ultima.getUserAgent() != null ? ultima.getUserAgent() : JSONObject.NULL);
				u.put("vigente", ExpiracionSesion.motivoVencimiento(ultima, ahora) == null);
				sesiones.put("ultima", u);
			} else
				sesiones.put("ultima", JSONObject.NULL);
			json.put("sesiones", sesiones);

			usuarios.put(json);
		}

		JSONObject seguridad = new JSONObject();
		seguridad.put("accesoEmpresa", AccesoEmpresa.modo().name());
		seguridad.put("expiracionSesion", ExpiracionSesion.modo().name());
		seguridad.put("inactividadMinutos", ExpiracionSesion.inactividadMinutos());
		seguridad.put("duracionMaximaHoras", ExpiracionSesion.duracionMaximaHoras());

		JSONObject result = new JSONObject();
		result.put("seguridad", seguridad);
		result.put("usuarios", usuarios);
		return json(result.toString());
	}

	/** Maximo de sesiones devueltas por usuario (las mas recientes); el total va aparte */
	private static final int MAX_SESIONES = 100;

	/**
	 * Sesiones de un usuario, de la mas reciente a la mas vieja, con su estado segun las reglas de expiracion. Solo
	 * ADMIN, siempre. No devuelve el token (es la credencial de la sesion).
	 */
	public Promise<Result> getSesionesUsuario(int uid) throws APIException {
		User user = User.findById(uid, true);
		Date ahora = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");

		List<Session> todas = Session.findByUser(user);
		JSONArray sesiones = new JSONArray();
		for (Session session : todas.subList(0, Math.min(MAX_SESIONES, todas.size()))) {
			String motivo = ExpiracionSesion.motivoVencimiento(session, ahora);
			String estado = !Boolean.TRUE.equals(session.isValid()) ? "CERRADA"
					: motivo == null ? "VIGENTE" : motivo.startsWith("duracion") ? "VENCIDA_DURACION" : "VENCIDA_INACTIVIDAD";
			JSONObject json = new JSONObject();
			json.put("creada", session.getCreationTimestamp() != null ? sdf.format(session.getCreationTimestamp()) : JSONObject.NULL);
			json.put("ultimoAcceso", session.getLastAccess() != null ? sdf.format(session.getLastAccess()) : JSONObject.NULL);
			json.put("ip", session.getIp() != null ? session.getIp() : JSONObject.NULL);
			json.put("userAgent", session.getUserAgent() != null ? session.getUserAgent() : JSONObject.NULL);
			json.put("estado", estado);
			sesiones.put(json);
		}

		JSONObject result = new JSONObject();
		result.put("userId", user.getId());
		result.put("total", todas.size());
		result.put("sesiones", sesiones);
		return json(result.toString());
	}

	/**
	 * Usuarios con acceso a la empresa: los ADMIN (acceso total) y los que tienen el permiso de la empresa.
	 */
	public Promise<Result> getUsuariosEmpresa(String rut) throws APIException {
		Empresa.findByRUT(rut, true);

		String permisoId = AccesoEmpresa.permisoEmpresa(rut);
		Map<Integer, JSONObject> usuarios = new LinkedHashMap<Integer, JSONObject>();

		for (User user : User.find(Role.ADMIN))
			usuarios.put(user.getId(), usuarioJson(user, "ADMIN"));

		// findWithPermissionId busca por prefijo: se filtra por permiso exacto
		for (User user : User.findWithPermissionId(permisoId))
			if (!usuarios.containsKey(user.getId()) && user.hasPermission(permisoId))
				usuarios.put(user.getId(), usuarioJson(user, "PERMISO"));

		JSONArray result = new JSONArray();
		for (JSONObject usuario : usuarios.values())
			result.put(usuario);

		return json(result.toString());
	}

	private static JSONObject usuarioJson(User user, String acceso) {
		JSONObject json = new JSONObject();
		json.put("id", user.getId());
		json.put("emailAddress", user.getEmailAddress());
		json.put("firstName", user.getFirstName() != null ? user.getFirstName() : JSONObject.NULL);
		json.put("lastName", user.getLastName() != null ? user.getLastName() : JSONObject.NULL);
		json.put("role", user.getRole().name());
		json.put("acceso", acceso);
		return json;
	}

	/**
	 * Da acceso a la empresa. Idempotente.
	 */
	public Promise<Result> agregarEmpresa(int uid, String rut) throws APIException {
		User user = User.findById(uid, true);
		Empresa.findByRUT(rut, true);

		String permisoId = AccesoEmpresa.permisoEmpresa(rut);
		if (!user.hasPermission(permisoId)) {
			Permission permiso = new Permission();
			permiso.setPermissionId(permisoId);
			permiso.setDescription("Acceso a la empresa " + rut);
			user.getPermissions().add(permiso);
			user.update();
			logger.info("Usuario " + user.getEmailAddress() + " recibe acceso a la empresa " + rut + " (otorgado por "
					+ PermissionValidator.getSessionUser(ctx()).getEmailAddress() + ")");
		}

		return json(OK);
	}

	/**
	 * Quita el acceso a la empresa. Idempotente.
	 */
	public Promise<Result> quitarEmpresa(int uid, String rut) throws APIException {
		User user = User.findById(uid, true);

		String permisoId = AccesoEmpresa.permisoEmpresa(rut);
		for (Iterator<Permission> iterator = user.getPermissions().iterator(); iterator.hasNext();) {
			Permission permiso = iterator.next();
			if (permisoId.equalsIgnoreCase(permiso.getPermissionId())) {
				iterator.remove();
				user.update();
				permiso.delete();
				logger.info("Usuario " + user.getEmailAddress() + " pierde acceso a la empresa " + rut + " (quitado por "
						+ PermissionValidator.getSessionUser(ctx()).getEmailAddress() + ")");
			}
		}

		return json(OK);
	}
}
