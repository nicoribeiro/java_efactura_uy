package com.bluedot.efactura.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.User;

import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Decide que control aplicar segun la ruta pedida:
 * <ul>
 * <li>/api/v1/init y /api/v1/users/{uid}/empresas...: solo ADMIN, siempre (sin importar el modo)</li>
 * <li>POST /api/v1/empresa, /api/v1/empresa/cargar, /api/v1/modo/..., /api/v1/pruebas/...: solo ADMIN</li>
 * <li>/api/v1/empresa/{rut}/...: acceso a esa empresa</li>
 * <li>el resto (GET /api/v1/empresa se filtra en el controlador, /rut, /pais): sin control adicional</li>
 * </ul>
 */
public class EmpresaAccessAction extends Action<EmpresaAccess> {

	private static final Pattern EMPRESA = Pattern.compile("^/api/v1/empresa/([^/]+)(/.*)?$");
	private static final Pattern SOLO_ADMIN = Pattern.compile("^/api/v1/(empresa/cargar|modo/.*|pruebas/.*)$");
	/** Siempre ADMIN: si dependiera del modo, en modo log cualquiera podria asignarse empresas antes de pasar a enforce */
	private static final Pattern SIEMPRE_ADMIN = Pattern.compile("^/api/v1/(init|users/[^/]+/empresas)(/.*)?$");

	@Override
	public Promise<Result> call(Context ctx) throws Throwable {
		String method = ctx.request().method();
		String path = ctx.request().path();
		String operacion = method + " " + path;

		boolean siempreAdmin = SIEMPRE_ADMIN.matcher(path).matches();

		// En modo off no se consulta el usuario (salvo las rutas que siempre exigen ADMIN)
		if (!siempreAdmin && AccesoEmpresa.modo() == AccesoEmpresa.Modo.OFF)
			return delegate.call(ctx);

		User user = PermissionValidator.getSessionUser(ctx);

		if (siempreAdmin)
			AccesoEmpresa.verificarAdmin(user, operacion, true);
		else if (SOLO_ADMIN.matcher(path).matches() || ("POST".equals(method) && "/api/v1/empresa".equals(path)))
			AccesoEmpresa.verificarAdmin(user, operacion, false);
		else {
			Matcher matcher = EMPRESA.matcher(path);
			if (matcher.matches())
				AccesoEmpresa.verificarEmpresa(user, matcher.group(1), operacion);
		}

		return delegate.call(ctx);
	}
}
