package com.bluedot.efactura.global;

import java.io.IOException;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;

import play.Logger;
import play.Play;
import play.i18n.Messages;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;




public class Secured extends Security.Authenticator
{

	public static final String AUTH_HEADER = "X-EFACTURA-AUTH-TOKEN";

	@Override
	public String getUsername(Context ctx)
	{
		boolean shouldByPassSecurity = checkSecurityExceptions(ctx);
		
		if(shouldByPassSecurity)
			return "";
		
		return doTokenAuth(ctx);
		
	}
	
	
	
	private String doTokenAuth(Context ctx)
	{
		String authToken = ctx.request().getHeader(AUTH_HEADER);
				
		if (authToken == null || "".equals(authToken))
		{
			authToken = ctx.request().getQueryString("api_key");
			if (authToken == null || "".equals(authToken)){
				ctx.args.put("error_cause", Messages.get("auth_token_not_found"));
				return null;
			}
		}
		
		String apikey = Play.application().configuration().getString("security.apikey", "12345");
		
		if (apikey.equals(authToken))
			return "efactura";
		else{
			ctx.args.put("error_cause", Messages.get("auth_token_invalid"));
			return null;
		}
	}
	
	

	@Override
	public Result onUnauthorized(Context ctx)
	{
		return unauthorized(ctx.args.get("error_cause").toString());
	}
	
	private boolean checkSecurityExceptions(Context ctx)
	{
		String exceptions = Play.application().configuration().getString("security.exceptions", "[]");
		
		try
		{
			JSONArray array = new JSONArray(exceptions);
			String endpoint = ctx.request().path();
			Logger.info("Endpoint is: " + endpoint);
			
			for(int i = 0; i< array.length(); i++)
			{
				String endpointToSkip = array.getString(i);
				
				if(Pattern.matches(endpointToSkip, endpoint))
					return true;
			}
			
		} catch (JSONException e)
		{
			e.printStackTrace();
		}
		return false;
	}
}
