package com.bluedot.efactura.global;

import com.bluedot.commons.security.Secured;

import play.mvc.Http.Request;
import play.mvc.Http.RequestHeader;

public class RequestUtils
{
	public static final String FORWARD_HEADER = "X-Forwarded-For";

	public static boolean isForwardedRequest(RequestHeader request)
	{
		return request.getHeader(FORWARD_HEADER) != null;
	}

	public static String requestAddress(RequestHeader request)
	{
		return isForwardedRequest(request) ? request.getHeader(FORWARD_HEADER) : request.remoteAddress();
	}
	
	public static boolean isHTTP(Request request)
	{
		return request.host().contains(":9000");
	}
	
	public static boolean isHMACSHA256Request(Request request)
	{
		return request.headers().containsKey(Secured.HMAC_KEY_HEADER) && 
				request.headers().containsKey(Secured.HMAC_SIGNATURE_HEADER) && 
				request.headers().containsKey(Secured.HMAC_NONCE_HEADER);
	}
	
}
