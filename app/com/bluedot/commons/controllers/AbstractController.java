package com.bluedot.commons.controllers;


import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.PermissionNames;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.PromiseCallback;
import com.bluedot.commons.security.User;
import com.google.inject.Provider;

import play.Application;
import play.db.jpa.JPAApi;
import play.mvc.Controller;
import play.mvc.Http.Context;
import play.mvc.Result;

public class AbstractController extends Controller
{

	protected JPAApi jpaApi;
	protected Provider<Application> application;
	
	public AbstractController(JPAApi jpaApi, Provider<Application> application) {
		super();
		this.jpaApi = jpaApi;
		this.application = application;
	}
	
	public static String OK = "{\"result_code\":0}";
	
	protected CompletionStage<Result> json(String jsonText)
	{
		return CompletableFuture.completedFuture(ok(jsonText).as("application/json")); 
	}
	
	protected CompletionStage<Result> accountAction(int accountId, PromiseCallback block) throws APIException
	{
		Account account = Account.findById(jpaApi, accountId, true);
		return PermissionValidator.runIfHasAccountAccess(jpaApi, Context.current(), block, account);
	}
	
	protected CompletionStage<Result> accountAction(int accountId, PromiseCallback block, PermissionNames permission) throws APIException
	{
		Account account = Account.findById(jpaApi, accountId, true);
		return PermissionValidator.runIfHasAccountAccess(jpaApi, Context.current(), block, account, permission, accountId + "");
	}
	
	protected CompletionStage<Result> userAction(int userId, PromiseCallback block) throws APIException
	{
		User user = User.findById(jpaApi, userId, true);
		return PermissionValidator.runIfHasUserAccess(jpaApi, Context.current(), block, user);
	}

	

}
