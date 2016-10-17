package com.bluedot.commons.serializers;

import com.bluedot.commons.security.Session;
import com.bluedot.commons.security.Settings;

public class JSONSerializerProvider {

	public static JSONSerializer<Settings> getSettingsSerializer() {
		return new SettingsSerializer<Settings>();
	}

	public static JSONSerializer<Session> getSessionSerializer() {
		return new SessionSerializer<Session>();
	}

}
