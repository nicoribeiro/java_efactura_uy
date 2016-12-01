package com.bluedot.commons.error;

import play.*;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.libs.Json;
import play.mvc.Http.*;
import play.mvc.*;

import javax.inject.*;

import com.bluedot.efactura.global.RequestUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {
	
	@Inject
    public ErrorHandler(Configuration configuration, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
    }

//	public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
//        return CompletableFuture.completedFuture(
//                Results.status(statusCode, "A client error occurred: " + message)
//        );
//    }

//    public CompletionStage<Result> onServerError(RequestHeader request, Throwable exception) {
//        return CompletableFuture.completedFuture(
//                Results.internalServerError("A server error occurred: " + exception.getMessage())
//        );
//    }
    
//    @Override
//    protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
//        return CompletableFuture.completedFuture(Results.forbidden(views.html.defaultpages.unauthorized.render()));
//    }
    
    protected static ObjectNode buildError(final String msg, int statusCode, String detail)
	{
		Context ctx = Context.current();
		ObjectNode jsonError = Json.newObject();
		jsonError.put("result_code", statusCode);
		jsonError.put("result_message", msg);
		jsonError.put("result_detail", detail != null?detail:"");
//		logger.error(RequestUtils.requestAddress(ctx.request()) + ": " + ctx.request().method() + " " + ctx.request().path() + " "+statusCode+" " + " " + ctx.request().getHeader("User-Agent")+" "+msg + " " + detail);
		
		return jsonError;
	}
}

