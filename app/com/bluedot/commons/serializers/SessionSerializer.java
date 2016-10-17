package com.bluedot.commons.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.Session;

public class SessionSerializer<T> extends JSONSerializer<Session> {

	@Override
	public JSONObject objectToJson(Session session) throws JSONException {
		JSONObject sessionJSON = new JSONObject();

		sessionJSON.put("creationTimestamp", session.getCreationTimestamp());
		sessionJSON.put("ip", session.getIp());
		sessionJSON.put("lastAccess", session.getLastAccess());
		sessionJSON.put("token", session.getToken());
		sessionJSON.put("userAgent", session.getUserAgent());
		sessionJSON.put("valid", session.isValid());

		return sessionJSON;
	}

}
