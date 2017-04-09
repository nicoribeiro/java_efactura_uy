import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.global.RequestUtils;

import play.Configuration;
import play.Environment;
import play.api.OptionalSourceMapper;
import play.api.UsefulException;
import play.api.routing.Router;
import play.http.DefaultHttpErrorHandler;
import play.mvc.Http.Context;
import play.mvc.Http.RequestHeader;
import play.mvc.Result;
import play.mvc.Results;

@Singleton
public class ErrorHandler extends DefaultHttpErrorHandler {

	
	final static Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
	
    @Inject
    public ErrorHandler(Configuration configuration, Environment environment,
                        OptionalSourceMapper sourceMapper, Provider<Router> routes) {
        super(configuration, environment, sourceMapper, routes);
    }
	

    protected CompletionStage<Result> onForbidden(RequestHeader request, String message) {
        return CompletableFuture.completedFuture(
                Results.forbidden("You're not allowed to access this resource.")
        );
    }

	@Override
	public CompletionStage<Result> onClientError(RequestHeader request, int statusCode, String message) {
		return CompletableFuture.completedFuture(
                Results.status(statusCode, "A client error occurred: " + message)
        );
    }

	@Override
	protected CompletionStage<Result> onProdServerError(RequestHeader request, UsefulException exception) {
		 if( exception.cause instanceof APIException)
	        	return handleAPIException((APIException)exception.cause);
		 
		return CompletableFuture.completedFuture(
				Results.internalServerError("A server error occurred: " + exception.getMessage())
        );
    }
	
	@Override
	protected CompletionStage<Result> onDevServerError(RequestHeader request, UsefulException exception) {
        if( exception.cause instanceof APIException)
        	return handleAPIException((APIException)exception.cause);
        
		return CompletableFuture.completedFuture(
                Results.internalServerError("A server error occurred: " + exception.getMessage())
        );
    }
    
	public static CompletionStage<Result> handleAPIException(APIException apiException)
	{
		Context ctx = Context.current();
    	
    	logger.error(RequestUtils.requestAddress(ctx.request()) + ": " + ctx.request().method() + " " + ctx.request().path() + " "+apiException.getError().httpCode()+" " + " " + ctx.request().getHeader("User-Agent")+" " + apiException.toString());
    	
    	return CompletableFuture.completedFuture(Results.status(apiException.getError().httpCode(), apiException.getJsonNode()));
	}
	
}

