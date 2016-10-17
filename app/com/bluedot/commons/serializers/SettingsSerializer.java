package com.bluedot.commons.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.AccountAccessLevel;
import com.bluedot.commons.security.Settings;



public class SettingsSerializer<T> extends JSONSerializer<Settings>
{
	private AccountAccessLevel accountACL = null;
	
	private String namespace = null;
	
	public SettingsSerializer() {

	}

	public SettingsSerializer<T> withACL(AccountAccessLevel accountACL)
	{
		this.accountACL = accountACL;
		return this;
	}
	
	
	public SettingsSerializer<T> withNamespace(String namespace)
	{
		this.namespace = namespace;
		return this;
	}

	@Override
	public JSONObject objectToJson(Settings settings) throws JSONException
	{
		if (accountACL != null)
			switch (accountACL)
			{
			case ADMIN:
			case VIEWER:
				return settings.getJsonSettingsObject();
			default:
				break;
			}
		
		if(namespace != null)
			return settings.getJsonSettingsObject(namespace);
		
		return settings.getJsonSettingsObject();
	}

}
