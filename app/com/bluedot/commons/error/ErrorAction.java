package com.bluedot.commons.error;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.global.RequestUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;


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

	@Override
	public Promise<Result> call(final Context ctx) throws Throwable
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
			ObjectNode jsonError = buildError(e.getError().message(), e.getError().code(), e.getDetailMessage());
			return Promise.<Result>pure(Results.status(e.getError().httpCode(), jsonError));
		}
		return null;
	}
	
	public static Promise<Result> handleAPIException(APIException e)
	{
		ObjectNode jsonError = buildError(e.getError().message(), e.getError().code(), e.getDetailMessage());
		e.printStackTrace();
		return Promise.<Result> pure(Results.status(e.getError().httpCode(), jsonError));
	}
	
	public static Promise<Result> handleUnknownException(Throwable e)
	{
		ObjectNode jsonError = buildError(e.getLocalizedMessage(), 500);
		e.printStackTrace();
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
		logger.error(RequestUtils.requestAddress(ctx.request()) + ": " + ctx.request().method() + " " + ctx.request().path() + " "+statusCode+" " + " " + ctx.request().getHeader("User-Agent")+" "+msg + " " + detail);
		
		return jsonError;
	}

}
