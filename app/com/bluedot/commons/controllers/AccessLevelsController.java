package com.bluedot.commons.controllers;

import java.util.concurrent.CompletionStage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.VerboseAction;
import com.bluedot.commons.security.AccountAccessLevel;
import com.bluedot.commons.security.Secured;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.play4jpa.jpa.db.Tx;

import play.Application;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;

@With(VerboseAction.class)
@Tx
@Transactional
@Security.Authenticated(Secured.class)
public class AccessLevelsController extends AbstractController
{
	
	@Inject
	public AccessLevelsController(JPAApi jpaApi, Provider<Application> application) {
		super(jpaApi, application);
	}

	public CompletionStage<Result> getAvailableAccountAccessLevels() throws APIException
	{
		JSONArray acls = new JSONArray();
		try{
			
			for(AccountAccessLevel acl : AccountAccessLevel.values())
			{
				if (!acl.hidden){
					JSONObject json = new JSONObject();
					json.put("friendlyName", acl.friendlyName);
					json.put("description", acl.description);
					json.put("value", acl);
					acls.put(json);
				}
			}
		}catch(JSONException e){
			throw APIException.raise(APIErrors.BAD_JSON).setDetailMessage("Bad json in account ACL");
		}
		
		return json(acls.toString());
	}
	
}
