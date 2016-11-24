package com.bluedot.commons.security;

import java.util.concurrent.CompletionStage;

import com.bluedot.commons.error.APIException;

import play.mvc.Result;

public interface PromiseCallback
{
	public CompletionStage<Result> execute() throws APIException;
}