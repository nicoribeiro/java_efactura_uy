package com.bluedot.commons.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.global.RequestUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;

import play.libs.F.Callback;
import play.libs.F.Function;
import play.libs.F.Promise;
import play.libs.Json;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Results;

/**
 * This class centralizes the exception handling of the system
 * in the API level.
 * 
 * Provides unified error response rendering, handling of
 * unchecked exceptions and handling of <code>HTTP.Status</code>
 * codes.
 * 
 * Also plays well with <code>JsonPostValidator</code>.
 * 
 * @author gonzox
 * @author nicoribeiro
 *
 */
public class ErrorAction extends Action<ErrorMessage>
{
	
	final static Logger logger = LoggerFactory.getLogger(ErrorAction.class);

	/** Log de acceso: una linea por request al terminar (logger "access", se puede mandar a otro archivo en logback) */
	final static Logger accessLogger = LoggerFactory.getLogger("access");

	/** Requests mas lentos que esto se loguean como WARN */
	private static final long LENTO_MS = 5000;

	/** Parametros del query string que son credenciales y no se escriben en el log */
	private static final java.util.regex.Pattern PARAMETROS_SENSIBLES = java.util.regex.Pattern
			.compile("(?i)([?&](?:api_key|token|auth_token|password|secret)=)[^&]*");

	@Override
	public Promise<Result> call(final Context ctx) throws Throwable
	{
		final long inicio = System.nanoTime();
		Promise<Result> result;
		try
		{
			result = procesar(ctx);
		} catch (Throwable t)
		{
			logAcceso(ctx, 500, inicio);
			throw t;
		}

		result.onFailure(new Callback<Throwable>()
		{
			@Override
			public void invoke(Throwable t)
			{
				logAcceso(ctx, 500, inicio);
			}
		});

		return result.map(new Function<Result, Result>()
		{
			@Override
			public Result apply(Result r)
			{
				logAcceso(ctx, r.toScala().header().status(), inicio);
				return r;
			}
		});
	}

	/**
	 * Una linea por request: metodo, URI (sin credenciales), status, duracion, usuario e IP. Nunca el cuerpo (puede
	 * tener datos fiscales o la contrasena del login).
	 */
	private static void logAcceso(Context ctx, int status, long inicio)
	{
		try
		{
			long ms = (System.nanoTime() - inicio) / 1000000;
			String uri = PARAMETROS_SENSIBLES.matcher(ctx.request().uri()).replaceAll("$1***");
			Object usuarioAutenticado = ctx.args.get(Secured.ACCESS_USER);
			String usuario = usuarioAutenticado != null ? usuarioAutenticado.toString() : ctx.request().username();
			String linea = ctx.request().method() + " " + uri + " -> " + status + " " + ms + "ms user="
					+ (usuario != null && !usuario.isEmpty() ? usuario : "-") + " ip=" + RequestUtils.requestAddress(ctx.request());

			if (status >= 500 || ms > LENTO_MS)
				accessLogger.warn((ms > LENTO_MS ? "LENTO " : "") + linea);
			else
				accessLogger.info(linea);
		} catch (RuntimeException e)
		{
			logger.debug("No se pudo escribir el log de acceso", e);
		}
	}

	private Promise<Result> procesar(final Context ctx) throws Throwable
	{
		Promise<Result> result = null;
		Promise<Result> error = null;
		try
		{
			error = checkErrorInContext(ctx);
			
			if(error != null)
				return error;
			
			if(this.delegate != null)
				result = this.delegate.call(ctx);
			
			error = checkErrorInContext(ctx);
			
			if(error != null)
				return error;
			
			Promise<Result> mappedResult = result.flatMap(new Function<Result, Promise<Result>>() 
			{
				@Override
				public Promise<Result> apply(Result r) throws Throwable
				{
					int statusCode = r.toScala().header().status();
					if (200 > statusCode || 400 < statusCode)
					{
						return handleExceptionWithStatusCode(ctx.args.get("error_cause").toString(), statusCode);
					}
					return Promise.<Result> pure(r);
				}
			});
			return mappedResult;
		}
		catch (APIException e)
		{
			result = handleAPIException(e);
		}
		catch (RuntimeException e)
		{
			if (e.getCause() instanceof APIException)
			{
				APIException apiEx = (APIException)e.getCause();
				result = handleAPIException(apiEx);
			}else{
				result = handleUnknownException(e);
			}
		}
		catch (Exception e)
		{
			result = handleUnknownException(e);
		}

		return result;
	}
	
	public Promise<Result> checkErrorInContext(Context ctx){
		if(ctx.args.get("validation_error") != null)
		{
			APIException e = (APIException)ctx.args.get("validation_exception");
			logger.warn("Validacion {}: {}{} ({} {})", e.getError().name(), e.getInternalMessage(),
					e.getDetailMessage() != null && !e.getDetailMessage().isEmpty() ? " - " + e.getDetailMessage() : "",
					ctx.request().method(), ctx.request().uri());
			ObjectNode jsonError = buildError(e.getInternalMessage(), e.getError().code(), e.getDetailMessage());
			return Promise.<Result>pure(Results.status(e.getError().httpCode(), jsonError));
		}
		return null;
	}
	
	public static Promise<Result> handleAPIException(APIException e)
	{
		ObjectNode jsonError = buildError(e.getInternalMessage(), e.getError().code(), e.getDetailMessage());
		if (e.isLog()) {
			if (esErrorInterno(e))
				logger.error("APIException is: ", e);
			else
				// Errores del cliente o de negocio (parametro invalido, no encontrado, sin acceso): una linea, sin stack
				logger.warn("APIException {}: {}{} ({})", e.getError().name(), e.getInternalMessage(),
						e.getDetailMessage() != null && !e.getDetailMessage().isEmpty() ? " - " + e.getDetailMessage() : "",
						requestActual());
		}
		return Promise.<Result> pure(Results.status(e.getError().httpCode(), jsonError));
	}

	/**
	 * Errores que indican un problema del servidor y necesitan el stack trace: los internos y cualquier APIException que
	 * envuelve otra excepcion
	 */
	private static boolean esErrorInterno(APIException e)
	{
		switch (e.getError()) {
		case INTERNAL_SERVER_ERROR:
		case WRAPPED_EXCEPTION:
		case NOT_IMPLEMENTED:
			return true;
		default:
			return e.getCause() != null;
		}
	}

	/** "GET /api/v1/..." del request en curso, para ubicar el error en el log */
	private static String requestActual()
	{
		try {
			Context ctx = Context.current();
			return ctx.request().method() + " " + ctx.request().uri();
		} catch (RuntimeException sinContexto) {
			return "sin request";
		}
	}
	
	public static Promise<Result> handleUnknownException(Throwable e)
	{
		ObjectNode jsonError = buildError(e.getLocalizedMessage(), 500);
		logger.error("UnknownException is: ", e);
		return Promise.<Result> pure(internalServerError(jsonError));
	}
	
	public static Promise<Result> handleExceptionWithStatusCode(String message, int statusCode)
	{
		ObjectNode jsonError = buildError(message, statusCode);
		return Promise.<Result> pure(status(statusCode, jsonError));
	}
	
	protected static ObjectNode buildError(final String msg, int statusCode)
	{
		return buildError(msg, statusCode, null);
	}
	
	protected static ObjectNode buildError(final String msg, int statusCode, String detail)
	{
		Context ctx = Context.current();
		ObjectNode jsonError = Json.newObject();
		jsonError.put("result_code", statusCode);
		jsonError.put("result_message", msg);
		jsonError.put("result_detail", detail != null?detail:"");
		// Detalle con User-Agent solo en DEBUG: el error ya queda en el log de acceso y en el WARN/ERROR de cada caso
		logger.debug(RequestUtils.requestAddress(ctx.request()) + ": " + ctx.request().method() + " " + ctx.request().path() + " "+statusCode+" " + " " + ctx.request().getHeader("User-Agent")+" "+msg + " " + detail);
		
		return jsonError;
	}

}
