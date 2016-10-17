package com.bluedot.commons.security;

import java.io.IOException;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.utils.Crypto;
import com.bluedot.efactura.global.RequestUtils;

import play.Play;
import play.i18n.Messages;
import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

public class Secured extends Security.Authenticator
{

	public static final String AUTH_HEADER = "AUTH-TOKEN";
	
	public static final String HMAC_KEY_HEADER = "ACCESS_KEY";
	public static final String HMAC_SIGNATURE_HEADER = "ACCESS_SIGNATURE";
	public static final String HMAC_NONCE_HEADER = "ACCESS_NONCE";
	
	public static final String HMAC_KEY_QS = "accessKey";
	public static final String HMAC_SIGNATURE_QS = "accessSignature";
	public static final String HMAC_NONCE_QS = "accessNonce";

	final static Logger logger = LoggerFactory.getLogger(Secured.class);
	
	@Override
	public String getUsername(Context ctx)
	{
		boolean shouldByPassSecurity = checkSecurityExceptions(ctx);
		
		if(shouldByPassSecurity)
			return "";
		
		if(RequestUtils.isHMACSHA256Request(ctx.request()))
		{
			return doHMACSHA256Auth(ctx);
		}else{
			return doSessionAuth(ctx);
		}
		
	}
	
	
	
	private String doSessionAuth(Context ctx)
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
		
		Session session =null;
		try
		{

			session = Session.findByAuthToken(authToken);
			
		} catch (Throwable e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (session != null && session.isValid())
		{
//			Account account = session.getUser().getMasterAccount();
//			if(account.getValidated()==null || !account.getValidated())
//			{
//				ctx.args.put("error_cause", Messages.get("account_invalid"));
//				return null;
//			}
				
			return session.getUser().getEmailAddress();
		} else
		{
			ctx.args.put("error_cause", Messages.get("auth_token_invalid"));
			return null;
		}
	}
	
	private String doHMACSHA256Auth(Context ctx)
	{
		String key = ctx.request().getHeader(HMAC_KEY_HEADER);
		
		if(key == null)
			key = ctx.request().getQueryString(HMAC_KEY_QS);
		
		String signature = ctx.request().getHeader(HMAC_SIGNATURE_HEADER);
		
		if(signature == null)
			signature = ctx.request().getQueryString(HMAC_SIGNATURE_QS);
		
		Long nonce;
		
		if (ctx.request().hasHeader(HMAC_NONCE_HEADER))
			nonce = Long.parseLong(ctx.request().getHeader(HMAC_NONCE_HEADER));
		else
			nonce = Long.parseLong(ctx.request().getQueryString(HMAC_NONCE_QS));
				
		if (key == null || "".equals(key))
		{
			ctx.args.put("error_cause", Messages.get("auth_token_not_found"));
			logger.info("HMAC auth request received. Key not found, nonce: " + nonce);
			return null;
		}
		
		logger.info("HMAC auth request received. Key: " + key + ", signature: " + signature + ", nonce: " + nonce);
		
		Credential credential = null;
		try
		{
			credential = Credential.findByKey(key);
		} catch (Throwable e)
		{
			e.printStackTrace();
		}
		
		if (credential != null && credential.isValid() && credential.getNonce() < nonce)
		{
			String protocol = "https";
			if(RequestUtils.isHTTP(ctx.request()))
		        protocol = "http";
			
			String url = protocol + "://" + ctx.request().host() + ctx.request().uri();
			
			
			
			String message = nonce + url + ((ctx.request().body() != null && ctx.request().body().asJson() != null)? ctx.request().body().asJson().toString() : "");
			
			
			logger.info("HMAC computing Signature for: " + message);
			
			boolean signatureMatches = false;
			try
			{
				signatureMatches = signature.equalsIgnoreCase(Crypto.HMACSHA256(message, credential.getSecret()));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		
			if (signatureMatches)
			{
				credential.setNonce(nonce);
				credential.update();
				
//				Account account = credential.getUser().getMasterAccount();
//				if(account.getValidated()==null || !account.getValidated())
//				{
//					ctx.args.put("error_cause", Messages.get("account_invalid"));
//					return null;
//				}
					
				return credential.getUser().getEmailAddress();
			}else{
				logger.info("HMAC signature not matching. Signature: " + credential.getSecret());
			}
		}
		
		ctx.args.put("error_cause", Messages.get("auth_token_invalid"));
		return null;
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
			logger.info("Endpoint is: " + endpoint);
			
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
