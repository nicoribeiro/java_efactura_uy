package com.bluedot.commons.serializers;


import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.Constants;
import com.bluedot.commons.security.User;


public class UserSerializer<T> extends JSONSerializer<User> {

	@Override
	public JSONObject objectToJson(User user) throws JSONException {
		JSONObject userJSON = new JSONObject();
		
		userJSON.put("id", user.getId());
		userJSON.put("firstName", user.getFirstName());
		userJSON.put("lastName", user.getLastName());
		userJSON.put("emailAddress", user.getEmailAddress());
		userJSON.put("phone", user.getPhone());
		userJSON.put("smsEnabled", user.getSettings().getBool(Constants.SMS_STATUS_MANAGEMENT_ENABLED) ?"Yes" : "No");
		userJSON.put("language", user.getSettings().has(Constants.MESSAGING_LANGUAGE) ? ("es".equals(user.getSettings().getString(Constants.MESSAGING_LANGUAGE))?"Spanish":"English") :"English");
		
				
		return userJSON;
	}

}
