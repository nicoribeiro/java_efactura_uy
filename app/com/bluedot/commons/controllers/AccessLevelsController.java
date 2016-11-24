package com.bluedot.commons.controllers;

import java.util.concurrent.CompletionStage;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.AccountAccessLevel;

import play.mvc.Result;

public class AccessLevelsController extends AbstractController
{
	
	public static CompletionStage<Result> getAvailableAccountAccessLevels() throws APIException
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
