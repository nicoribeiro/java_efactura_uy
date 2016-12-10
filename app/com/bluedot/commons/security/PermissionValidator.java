package com.bluedot.commons.security;

import java.text.MessageFormat;
import java.util.concurrent.CompletionStage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.User.Role;
import com.bluedot.efactura.global.RequestUtils;

import play.mvc.Http.Context;
import play.db.jpa.JPAApi;
import play.mvc.Result;

public class PermissionValidator
{

	final static Logger logger = LoggerFactory.getLogger(PermissionValidator.class);
	
	public static User getSessionUser(JPAApi jpaApi, Context ctx) throws APIException
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
				credential = Credential.findByKey(jpaApi, key, true);

				sessionUser = credential.getUser();

				if (sessionUser == null)
					throw APIException.raise(APIErrors.USER_NOT_FOUND.withParams("", ""));

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
				session = Session.findByAuthToken(jpaApi, authToken, true);

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
	
	public static CompletionStage<Result> runIfHasRole(JPAApi jpaApi, Context context, PromiseCallback executionBlock, Role role) throws APIException
	{
		User sessionUser = getSessionUser(jpaApi, Context.current());
		if (sessionUser.getRole() == (role))
			return executionBlock.execute();

		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}

	

	public static CompletionStage<Result> runWithValidation(JPAApi jpaApi, Context context, PromiseCallback executionBlock, PermissionNames permission, Object... args) throws APIException
	{
		if (permission == PermissionNames.ANY || permission == null)
			return executionBlock.execute();

		User sessionUser = getSessionUser(jpaApi, context);

		if (has(sessionUser, permission, args))
			return executionBlock.execute();

		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}
	
	
	public static CompletionStage<Result> runIfHasAccountAccess(JPAApi jpaApi, Context context, PromiseCallback executionBlock, Account account) throws APIException
	{
		return runIfHasAccountAccess(jpaApi, context, executionBlock, account, PermissionNames.ACCOUNT_ACCESS, account.getId()+"");
	}
	
	public static CompletionStage<Result> runIfHasAccountAccess(JPAApi jpaApi, Context context, PromiseCallback executionBlock, Account account, PermissionNames permission, Object... args) throws APIException
	{
		User sessionUser = getSessionUser(jpaApi, context);
		
		logger.info("Session user is: " + sessionUser);
		logger.info("Account is: " + account);
		
		if (sessionUser.isAdminOnAccount(account))
		{
			return executionBlock.execute();
		}
		
		return runWithValidation(jpaApi, context, executionBlock, permission, args);
	}

	private static String p(PermissionNames permissionName, Object... args)
	{
		return MessageFormat.format(permissionName.pattern, args);
	}

	public static boolean has(User sessionUser, PermissionNames permission, Object... args)
	{
		return sessionUser.hasPermission(p(permission, args));
	}


	public static CompletionStage<Result> runIfHasUserAccess(JPAApi jpaApi, Context current, PromiseCallback block, User user) throws APIException
	{
		User sessionUser = getSessionUser(jpaApi, current);
	
		if (sessionUser.getId()==user.getId())
			return block.execute();
		
		throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Operation nor permitted");
	}

}
