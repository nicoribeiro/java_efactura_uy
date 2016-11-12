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
	
	public static JSONObject merge(JSONObject Obj1, JSONObject Obj2){
		if (Obj1==null && Obj2!=null)
			return new JSONObject(Obj2, JSONObject.getNames(Obj2));
		
		if (Obj1!=null && Obj2==null)
			return new JSONObject(Obj1, JSONObject.getNames(Obj1));
		
		if (Obj1==null && Obj2==null)
			return new JSONObject();
		
		JSONObject merged;
		
		if (JSONObject.getNames(Obj1)==null)
			merged = new JSONObject();
		else
			merged = new JSONObject(Obj1, JSONObject.getNames(Obj1));
		
		for(String key : JSONObject.getNames(Obj2))
		{
		  merged.put(key, Obj2.get(key));
		}
		return merged;
	}
}
