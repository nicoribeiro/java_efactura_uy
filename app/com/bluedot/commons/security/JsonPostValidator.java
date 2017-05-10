package com.bluedot.commons.security;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.fasterxml.jackson.databind.JsonNode;

import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

/**
 * Json POST validation
 * 
 * Simple validator that stops the execution raising an exception when one of
 * the fields involved in the validation is not present in the json body of the
 * request.
 * 
 * If the request is a bad formed json an exception is also raised. This may
 * happen when a request comes without content type because our API assumes JSON
 * as the only message format.
 * 
 * This class is intended to use with the <code>ErrorMessage</code> annotation
 * which will handle the exception to format it as a readable json response to
 * the user.
 * 
 * To use with custom implementations see <code>ErrorMessageHandler</code>
 * class.
 * 
 * @author gonz
 * 
 */
public class JsonPostValidator extends Action<ValidateJsonPost>
{

	@Override
	public Promise<Result> call(Context context) throws Throwable
	{
		String[] values = configuration.fields();
		JsonNode json = context.request().body().asJson();

		if (json == null)
		{
			return raiseValidationError(context, APIException.raise(APIErrors.BAD_JSON));
		}

		for (int i = 0; i < values.length; i++)
		{
			String value = values[i];
			if (!json.has(value))
			{
				return raiseValidationError(context, APIException.raise(APIErrors.MISSING_PARAMETER.withParams(value)));
			}
		}

		return delegate.call(context);
	}

	/**
	 * This method raise an exception by calling the delegate's action who needs
	 * to handle the context result in a proper way.
	 * 
	 * The exception generated is set in the context under the key
	 * <code>validation_exception</code> along with a string flag
	 * <code>validation_error</code> that marks the response as erroneous.
	 * 
	 * When invoked, the delegate has the option to query the context and take
	 * an action if an error occurred. Otherwise the validation is hidden.
	 * 
	 * @param context
	 * @param exception
	 *            Is the concrete exception
	 * @return The promise of executing the delegate having validated the fields
	 *         before.
	 * @throws Throwable
	 */
	protected Promise<Result> raiseValidationError(Context context, APIException exception) throws Throwable
	{
		context.args.put("validation_error", "true");
		context.args.put("validation_exception", exception);
		return delegate.call(context);
	}

}
