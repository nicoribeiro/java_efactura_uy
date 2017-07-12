package com.bluedot.commons.security;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.User.Role;
import com.bluedot.efactura.global.RequestUtils;

import play.libs.F.Promise;
import play.mvc.Http.Context;
import play.mvc.Result;

public class PermissionValidator
{

	final static Logger logger = LoggerFactory.getLogger(PermissionValidator.class);
	
	public static User getSessionUser(Context ctx) throws APIException
	{
		User sessionUser = null;

		if (ctx.args.containsKey("sessionUser"))
			return (User) ctx.args.get("sessionUser");

		if (ctx.request().getHeader(Secured.HMAC_KEY_HEADER) == null && ctx.request().getHeader(Secured.AUTH_HEADER) == null)
			throw APIException.raise(APIErrors.NO_AUTH_METHOD_DEFINED);

		if (RequestUtils.isHMACSHA256Request(ctx.request()))
		{
			String key = ctx.request().getHeader(Secured.HMAC_KEY_HEADER);

			Credential credential = null;
			try
			{
				credential = Credential.findByKey(key, true);

				sessionUser = credential.getUser();

				if (sessionUser == null)
					throw APIException.raise(APIErrors.USER_NOT_FOUND).withParams("", "");

			} catch (Throwable e)
			{
				e.printStackTrace();
			}
		} else
		{
			String authToken = ctx.request().getHeader(Secured.AUTH_HEADER);

			if (authToken == null || "".equals(authToken))
				authToken = ctx.request().getQueryString("api_key");

			Session session = null;
			try
			{
				session = Session.findByAuthToken(authToken, true);

				sessionUser = session.getUser();

				if (sessionUser == null)
					throw APIException.raise(APIErrors.USER_NOT_FOUND);
			} catch (Throwable e)
			{
				e.printStackTrace();
			}
		}
		return sessionUser;
	}
	
	public static Promise<Result> runIfHasRole(Context context, PromiseCallback executionBlock, Role role) throws APIException
	{
		User sessionUser = getSessionUser(Context.current());
		if (sessionUser.getRole() == (role))
			return executionBlock.execute();

		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}

	

	public static Promise<Result> runWithValidation(Context context, PromiseCallback executionBlock, PermissionNames permission, Object... args) throws APIException
	{
		if (permission == PermissionNames.ANY || permission == null)
			return executionBlock.execute();

		User sessionUser = getSessionUser(context);

		if (has(sessionUser, permission, args))
			return executionBlock.execute();

		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}
	
	
	public static Promise<Result> runIfHasAccountAccess(Context context, PromiseCallback executionBlock, Account account) throws APIException
	{
		return runIfHasAccountAccess(context, executionBlock, account, PermissionNames.ACCOUNT_ACCESS, account.getId()+"");
	}
	
	public static Promise<Result> runIfHasAccountAccess(Context context, PromiseCallback executionBlock, Account account, PermissionNames permission, Object... args) throws APIException
	{
		User sessionUser = getSessionUser(context);
		
		logger.info("Session user is: " + sessionUser);
		logger.info("Account is: " + account);
		
		if (sessionUser.isAdminOnAccount(account))
		{
			return executionBlock.execute();
		}
		
		return runWithValidation(context, executionBlock, permission, args);
	}

	private static String p(PermissionNames permissionName, Object... args)
	{
		return MessageFormat.format(permissionName.pattern, args);
	}

	public static boolean has(User sessionUser, PermissionNames permission, Object... args)
	{
		return sessionUser.hasPermission(p(permission, args));
	}


	public static Promise<Result> runIfHasUserAccess(Context current, PromiseCallback block, User user) throws APIException
	{
		User sessionUser = getSessionUser(current);
	
		if (sessionUser.getId()==user.getId())
			return block.execute();
		
		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}

}
