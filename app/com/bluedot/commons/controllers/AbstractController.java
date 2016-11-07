package com.bluedot.commons.controllers;


import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.PromiseCallback;
import com.bluedot.commons.security.User;

import play.libs.F.Promise;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;

public class AbstractController extends Controller
{

	public static String OK = "{\"result_code\":0}";
	
	protected static Promise<Result> json(String jsonText)
	{
		return Promise.<Result> pure(ok(jsonText).as("application/json")); 
	}
	
	protected static Promise<Result> accountAction(int accountId, PromiseCallback block) throws APIException
	{
		Account account = Account.findById(accountId, true);
		return PermissionValidator.runIfHasAccountAccess(Context.current(), block, account);
	}
	
	protected static Promise<Result> accountAction(int accountId, PromiseCallback block, PermissionNames permission) throws APIException
	{
		Account account = Account.findById(accountId, true);
		return PermissionValidator.runIfHasAccountAccess(Context.current(), block, account, permission, accountId + "");
	}
	
	protected static Promise<Result> userAction(int userId, PromiseCallback block) throws APIException
	{
		User user = User.findById(userId, true);
		return PermissionValidator.runIfHasUserAccess(Context.current(), block, user);
	}

}
