package com.bluedot.commons.security;

import com.bluedot.commons.error.APIException;

import play.libs.F.Promise;
import play.mvc.Result;

public interface PromiseCallback
{
	public Promise<Result> execute() throws APIException;
}