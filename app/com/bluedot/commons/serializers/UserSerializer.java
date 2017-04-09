package com.bluedot.commons.serializers;


import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.Constants;
import com.bluedot.commons.security.User;

import play.db.jpa.JPAApi;


public class UserSerializer<T> extends JSONSerializer<User> {

	@Override
	public JSONObject objectToJson(JPAApi jpaApi, User user) throws JSONException {
		JSONObject userJSON = new JSONObject();
		
		userJSON.put("id", user.getId());
		userJSON.put("firstName", user.getFirstName());
		userJSON.put("lastName", user.getLastName());
		userJSON.put("emailAddress", user.getEmailAddress());
		userJSON.put("phone", user.getPhone());
		userJSON.put("smsEnabled", user.getSettings(jpaApi).getBool(jpaApi, Constants.SMS_STATUS_MANAGEMENT_ENABLED) ?"Yes" : "No");
		userJSON.put("language", user.getSettings(jpaApi).has(jpaApi, Constants.MESSAGING_LANGUAGE) ? ("es".equals(user.getSettings(jpaApi).getString(jpaApi, Constants.MESSAGING_LANGUAGE))?"Spanish":"English") :"English");
		
				
		return userJSON;
	}

}
