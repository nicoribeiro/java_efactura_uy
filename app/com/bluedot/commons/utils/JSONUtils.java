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
}
