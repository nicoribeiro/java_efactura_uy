package com.bluedot.efactura.controllers;

import java.util.Iterator;

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
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

/**
 * Asignacion de empresas a usuarios (permiso access_empresa_{rut}). Solo ADMIN, siempre: lo exige
 * {@link EmpresaAccess} para /api/v1/users/{uid}/empresas sin importar seguridad.accesoEmpresa.
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
