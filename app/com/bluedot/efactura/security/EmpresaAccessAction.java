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
 * <li>/api/v1/init, /api/v1/usuarios, /api/v1/procesos, /api/v1/users/{uid}/empresas..., /api/v1/empresa/{rut}/usuarios,
 * POST /api/v1/empresa, /api/v1/empresa/cargar, /api/v1/modo/... y /api/v1/pruebas/...: solo ADMIN, siempre (sin importar el
 * modo)</li>
 * <li>/api/v1/empresa/{rut}/...: acceso a esa empresa</li>
 * <li>el resto (GET /api/v1/empresa se filtra en el controlador, /rut, /pais): sin control adicional</li>
 * </ul>
 */
public class EmpresaAccessAction extends Action<EmpresaAccess> {

	private static final Pattern EMPRESA = Pattern.compile("^/api/v1/empresa/([^/]+)(/.*)?$");
	/**
	 * Siempre ADMIN: si dependiera del modo, en modo log cualquiera podria asignarse empresas, crear una empresa, cambiar el
	 * modo del sistema o correr pruebas antes de pasar a enforce
	 */
	private static final Pattern SIEMPRE_ADMIN = Pattern
			.compile("^/api/v1/(init|usuarios|procesos|users/[^/]+/empresas|empresa/[^/]+/usuarios|empresa/cargar|modo|pruebas)(/.*)?$");

	/** true si la ruta exige ADMIN en cualquier modo de seguridad.accesoEmpresa */
	static boolean exigeAdminSiempre(String method, String path) {
		return SIEMPRE_ADMIN.matcher(path).matches() || ("POST".equals(method) && "/api/v1/empresa".equals(path));
	}

	@Override
	public Promise<Result> call(Context ctx) throws Throwable {
		String method = ctx.request().method();
		String path = ctx.request().path();
		String operacion = method + " " + path;

		boolean siempreAdmin = exigeAdminSiempre(method, path);

		// En modo off no se consulta el usuario (salvo las rutas que siempre exigen ADMIN)
		if (!siempreAdmin && AccesoEmpresa.modo() == AccesoEmpresa.Modo.OFF)
			return delegate.call(ctx);

		User user = PermissionValidator.getSessionUser(ctx);

		if (siempreAdmin)
			AccesoEmpresa.verificarAdmin(user, operacion, true);
		else {
			Matcher matcher = EMPRESA.matcher(path);
			if (matcher.matches())
				AccesoEmpresa.verificarEmpresa(user, matcher.group(1), operacion);
		}

		return delegate.call(ctx);
	}
}
