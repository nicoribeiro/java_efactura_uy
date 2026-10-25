package com.bluedot.commons.utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class JSONUtils
{
	public static boolean isValidJSON(String test)
	{
	    try 
	    {
	        new JSONObject(test);
	    } 
	    catch(JSONException ex) 
	    {
	        try 
	        {
	            new JSONArray(test);
	        } 
	        catch(JSONException ex1) 
	        {
	            return false;
	        }
	    }
	    return true;
	}
	
	/**
	 * Copia de Obj1 con las claves de Obj2 encima (Obj2 pisa). Cualquiera puede ser null o vacio: getNames de un objeto
	 * vacio devuelve null (antes daba NullPointerException).
	 */
	public static JSONObject merge(JSONObject Obj1, JSONObject Obj2){
		JSONObject merged = new JSONObject();
		for (JSONObject obj : new JSONObject[] { Obj1, Obj2 }) {
			String[] names = obj != null ? JSONObject.getNames(obj) : null;
			if (names != null)
				for (String key : names)
					merged.put(key, obj.get(key));
		}
		return merged;
	}
	
	public static JSONObject createObjectList(JSONArray list, long rowCount, int page, int pageSize) {
		JSONObject result = new JSONObject();
		
		result.put("results", list);
		result.put("count", rowCount);
		result.put("page", page);
		result.put("pageSize", pageSize);
		
		return result;
	}
	
	public static JSONArray concatArray(JSONArray... arrs) throws JSONException {
	    JSONArray result = new JSONArray();
	    for (JSONArray arr : arrs) {
	        for (int i = 0; i < arr.length(); i++) {
	            result.put(arr.get(i));
	        }
	    }
	    return result;
	}
}
