package com.bluedot.commons.serializers;

import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public abstract class JSONSerializer<T>
{
	protected boolean includePrivateFields = false;
	
	protected SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
	
	public JSONObject objectToJson(T object) throws JSONException
	{
		return objectToJson(object, false);
	}
	
	public abstract JSONObject objectToJson(T object, boolean shrinkSerializarion) throws JSONException;
	
	public JSONArray objectToJson(List<T> list) throws JSONException
	{
		return objectToJson(list, false);
	}
	
	public JSONArray objectToJson(List<T> list, boolean shrinkSerializarion) throws JSONException
	{
		JSONArray array = new JSONArray();
		
		if (list==null || list.isEmpty())
			return array;
		
		for (Iterator<T> iterator = list.iterator(); iterator.hasNext();)
		{
			T t = iterator.next();
			JSONObject object = objectToJson(t, shrinkSerializarion);
			array.put(object);
		}
		return array;
	}
	
	public JSONSerializer<T> includePrivateFields()
	{
		includePrivateFields = true;
		return this;
	}
}
