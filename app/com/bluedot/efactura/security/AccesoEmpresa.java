package com.bluedot.efactura.security;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.Permission;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;

import play.Play;

/**
 * Control de acceso por empresa.
 *
 * Un usuario con rol ADMIN accede a todo. El resto solo a las empresas para las que tiene el permiso
 * access_empresa_{rut} (PermissionNames.EMPRESA_ACCESS), que asigna un ADMIN con /api/v1/users/:uid/empresas.
 *
 * El comportamiento depende de seguridad.accesoEmpresa:
 * <ul>
 * <li>off: no se controla nada</li>
 * <li>log: se loguea quien seria bloqueado, sin bloquear</li>
 * <li>enforce (default, tambien si falta o es invalido): se bloquea con 403</li>
 * </ul>
 */
public class AccesoEmpresa {

	static final Logger logger = LoggerFactory.getLogger(AccesoEmpresa.class);

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	private static final String PREFIJO_PERMISO = PermissionNames.EMPRESA_ACCESS.pattern.replace("{0}", "");

	public static Modo modo() {
		String valor = Play.application().configuration().getString("seguridad.accesoEmpresa", "enforce");
		try {
			return Modo.valueOf(valor.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.error("Valor invalido para seguridad.accesoEmpresa: '" + valor + "', se usa 'enforce'");
			return Modo.ENFORCE;
		}
	}

	public static boolean esAdmin(User user) {
		return user != null && user.getRole() == Role.ADMIN;
	}

	public static String permisoEmpresa(String rut) {
		return MessageFormat.format(PermissionNames.EMPRESA_ACCESS.pattern, rut);
	}

	public static boolean tieneAcceso(User user, String rut) {
		return esAdmin(user) || (user != null && user.hasPermission(permisoEmpresa(rut)));
	}

	/**
	 * RUTs de las empresas a las que el usuario tiene permiso explicito (sin considerar el rol ADMIN)
	 */
	public static List<String> rutsPermitidos(User user) {
		List<String> ruts = new LinkedList<String>();
		if (user != null && user.getPermissions() != null)
			for (Permission permiso : user.getPermissions())
				if (permiso.getPermissionId() != null && permiso.getPermissionId().startsWith(PREFIJO_PERMISO))
					ruts.add(permiso.getPermissionId().substring(PREFIJO_PERMISO.length()));
		return ruts;
	}

	/**
	 * Verifica que el usuario pueda acceder a la empresa. Segun el modo, loguea o rechaza con 403.
	 */
	public static void verificarEmpresa(User user, String rut, String operacion) throws APIException {
		Modo modo = modo();
		if (modo == Modo.OFF || tieneAcceso(user, rut))
			return;

		logger.warn((modo == Modo.ENFORCE ? "BLOQUEADO" : "SERIA BLOQUEADO") + ": usuario "
				+ (user != null ? user.getEmailAddress() : "desconocido") + " sin acceso a la empresa " + rut + " ("
				+ operacion + ")");

		if (modo == Modo.ENFORCE)
			throw APIException.raise(APIErrors.ACCESO_EMPRESA_DENEGADO).setDetailMessage("RUT " + rut);
	}

	/**
	 * Verifica que el usuario sea ADMIN. Con siempre=true se exige aunque el modo sea off o log.
	 */
	public static void verificarAdmin(User user, String operacion, boolean siempre) throws APIException {
		if (esAdmin(user))
			return;

		Modo modo = siempre ? Modo.ENFORCE : modo();
		if (modo == Modo.OFF)
			return;

		logger.warn((modo == Modo.ENFORCE ? "BLOQUEADO" : "SERIA BLOQUEADO") + ": usuario "
				+ (user != null ? user.getEmailAddress() : "desconocido") + " no es ADMIN (" + operacion + ")");

		if (modo == Modo.ENFORCE)
			throw APIException.raise(APIErrors.SOLO_ADMIN);
	}
}
