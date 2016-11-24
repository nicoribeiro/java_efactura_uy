package com.bluedot.commons.error;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.efactura.global.RequestUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;


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
	public CompletionStage<Result> call(final Context ctx)
	{
		CompletableFuture<Result> result = null;
		CompletableFuture<Result> error = null;
		
			error = checkErrorInContext(ctx);
			
			if(error != null)
				return error;
			
			if(this.delegate != null)
				result = this.delegate.call(ctx).toCompletableFuture();
			
			error = checkErrorInContext(ctx);
			
			if(error != null)
				return error;
			
			CompletableFuture<Result> resultOK =  result.thenComposeAsync(( Result r)-> 
				{
					int statusCode = r.status();
					if (200 > statusCode || 400 < statusCode)
					{
						return handleExceptionWithStatusCode(ctx.args.get("error_cause").toString(), statusCode);
					}
					return CompletableFuture.completedFuture(r);
				});
			
			resultOK.exceptionally( ex->{
					if (ex instanceof APIException)
					{
						APIException apiEx = (APIException)ex.getCause();
						return handleAPIException(apiEx);
					}else{
						return handleUnknownException(ex);
					}
					
				});
			
			return resultOK;
		
	}
	
	public CompletableFuture<Result> checkErrorInContext(Context ctx){
		if(ctx.args.get("validation_error") != null)
		{
			APIException e = (APIException)ctx.args.get("validation_exception");
			ObjectNode jsonError = buildError(e.getError().message(), e.getError().code(), e.getDetailMessage());
			return CompletableFuture.completedFuture(Results.status(e.getError().httpCode(), jsonError));
		}
		return null;
	}
	
	public static Result handleAPIException(APIException e)
	{
		ObjectNode jsonError = buildError(e.getError().message(), e.getError().code(), e.getDetailMessage());
		if (e.isLog())
			logger.error("APIException is: ", e);
		return Results.status(e.getError().httpCode(), jsonError);
	}
	
	public static Result handleUnknownException(Throwable e)
	{
		ObjectNode jsonError = buildError(e.getLocalizedMessage(), 500);
		logger.error("UnknownException is: ", e);
		return internalServerError(jsonError);
	}
	
	public static CompletableFuture<Result> handleExceptionWithStatusCode(String message, int statusCode)
	{
		ObjectNode jsonError = buildError(message, statusCode);
		return CompletableFuture.completedFuture(status(statusCode, jsonError));
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
