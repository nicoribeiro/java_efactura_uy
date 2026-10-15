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

	/** Clave en ctx.args con el email del usuario autenticado (lo usa el log de acceso de ErrorAction) */
	public static final String ACCESS_USER = "access_user";
	
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
			if (!ExpiracionSesion.verificar(session, ctx.request().method() + " " + ctx.request().path()))
			{
				ctx.args.put("error_cause", "La sesion vencio, vuelva a ingresar");
				return null;
			}

//			Account account = session.getUser().getMasterAccount();
//			if(account.getValidated()==null || !account.getValidated())
//			{
//				ctx.args.put("error_cause", Messages.get("account_invalid"));
//				return null;
//			}
				
			if (!usuarioHabilitado(ctx, session.getUser(), true))
				return null;

			// Para el log de acceso: Security.Authenticated borra el username del request al terminar
			ctx.args.put(ACCESS_USER, session.getUser().getEmailAddress());
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
		
		// Nunca loguear la key entera, la firma ni el secret: solo el comienzo de la key
		logger.debug("HMAC auth request received. Key: " + prefijo(key) + ", nonce: " + nonce);
		
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
			
			
			logger.debug("HMAC computing Signature for " + url + " (nonce " + nonce + ", " + message.length() + " caracteres)");
			
			boolean signatureMatches = false;
			try
			{
				signatureMatches = signature.equalsIgnoreCase(Crypto.HMACSHA256(message, credential.getSecret()));
			} catch (IOException e)
			{
				e.printStackTrace();
			}
		
			if (signatureMatches && !usuarioHabilitado(ctx, credential.getUser(), false))
				return null;

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
					
				ctx.args.put(ACCESS_USER, credential.getUser().getEmailAddress());
				return credential.getUser().getEmailAddress();
			}else{
				// Nunca loguear el secret de la credencial
				logger.warn("HMAC: la firma no coincide para la key " + prefijo(key) + " (nonce " + nonce + ", " + ctx.request().method() + " "
						+ ctx.request().path() + ")");
			}
		} else if (credential == null || !credential.isValid())
			logger.warn("HMAC: key " + prefijo(key) + " inexistente o revocada (" + ctx.request().method() + " " + ctx.request().path() + ")");
		else
			logger.warn("HMAC: nonce " + nonce + " no es mayor al ultimo usado (" + credential.getNonce() + ") para la key " + prefijo(key) + " ("
					+ ctx.request().method() + " " + ctx.request().path() + ")");
		
		ctx.args.put("error_cause", Messages.get("auth_token_invalid"));
		return null;
	}

	/** Comienzo de la key, para el log (la key entera no se loguea) */
	private static String prefijo(String key) {
		return key == null ? null : key.length() <= 8 ? key : key.substring(0, 8) + "...";
	}

	/**
	 * Rutas que se pueden usar con una contrasena temporal o sin el email confirmado (para poder confirmarlo y cambiarla)
	 */
	private static final java.util.regex.Pattern CUENTA = java.util.regex.Pattern
			.compile("^/api/v1/cuenta(/password|/email/codigo|/email/confirmar)?$");

	/**
	 * Usuario desactivado: rechazado (sesion o HMAC). Con contrasena temporal y sesion: solo puede usar /api/v1/cuenta
	 * (ver su perfil y cambiar la contrasena).
	 */
	private boolean usuarioHabilitado(Context ctx, User user, boolean porSesion)
	{
		if (user == null)
			return true;
		if (!user.isActivo())
		{
			logger.info("Acceso rechazado: usuario desactivado " + user.getEmailAddress());
			ctx.args.put("error_cause", "usuario_desactivado");
			return false;
		}
		if (porSesion && user.isDebeConfirmarEmail() && !user.isValidated() && !CUENTA.matcher(ctx.request().path()).matches())
		{
			ctx.args.put("error_cause", "email_confirmation_required");
			return false;
		}
		if (porSesion && user.isDebeCambiarPassword() && !CUENTA.matcher(ctx.request().path()).matches())
		{
			ctx.args.put("error_cause", "password_change_required");
			return false;
		}
		return true;
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
			logger.debug("Endpoint is: " + endpoint);
			
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
