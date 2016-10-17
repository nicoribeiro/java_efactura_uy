package com.bluedot.commons.error;


import java.util.Iterator;
import java.util.List;

import play.i18n.Messages;




public class APIException extends Throwable
{

	private APIException(APIErrors error, Throwable cause) {
		super("", cause);
		this.error = error;
	}
	
	private APIException(APIErrors error) {
		super(error.message);
		this.error = error;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2194392490942571005L;
	
	private String detailMessage= "";
	private APIErrors error;
	
	/*
	 * 400 Bad Request
	 * 401 Unauthorized
	 * 402 Payment Required
	 * 403 Forbidden
	 * 404 Not Found
	 * 405 Method Not Allowed
	 * 406 Not Acceptable
	 * 407 Proxy Authentication Required
	 * 408 Request Timeout
	 * 409 Conflict
	 * 410 Gone
	 * 411 Length Required
	 * 412 Precondition Failed
	 */
	
	public enum APIErrors
	{
		
		/*
		 * General Errors
		 */
		BAD_PARAMETER_VALUE(1, 404), 
		MISSING_PARAMETER(2, 404), 
		NOT_SUPPORTED(3,404), 
		INTERNAL_SERVER_ERROR(4, 500),
		NOT_IMPLEMENTED(5,  501), 
		UNAUTHORIZED(6,  401),
		BAD_JSON(7,  400),
		
		/*
		 * This error is used when the APIexception is acting as a wrapper for another exception.
		 */
		WRAPPED_EXCEPTION(8, 404), 
		 
		
		
		/*
		 * Commons Errors
		 */
		ACCOUNT_NOT_FOUND(9,  404),
		USER_NOT_FOUND(10,  404),
		ALERT_NOT_FOUND(11,  404),
		CREDENTIAL_NOT_FOUND(12,  404), 
		SESSION_NOT_FOUND(13,  404),
		INVALID_VALIDATION_KEY(14,  404),
		NO_AUTH_METHOD_DEFINED(15,  404),
		HAZELCAST(16,  500),
		USER_NOT_PART_OF_ACCOUNT(17,  406),
		USER_ALREADY_EXISTS(18,  409),
		EMAIL_EXISTS(19,  409), 
		PHONE_EXISTS(20,  409),
		SETTING_SCHEMA_ERROR(21,  404),
		
		
		
		 
		
		
		/*
		 * Exceptions de facturacion electronica
		 */
		CAE_DATA_NOT_FOUND(100, 404),
		CAE_NOT_AVAILABLE_ID(101,404), 
		MALFORMED_CFE(102,404), 
		PAIS_NO_ENCONTRADO(103, 404),
		EMPRESA_NO_ENCONTRADA(104, 404),
		ERROR_COMUNICACION_DGI(105,  404), 
		REPORTE_DIARIO_NO_ENCONTRADO(106,  404),
		NO_CFE_CREATED(107,  404),
		CFE_NO_ENCONTRADO(108,  404),
		SOBRE_NO_ENCONTRADO(109,  404), 
		UI_NO_ENCONTRADA(110,  404), 
		NO_REPORT_CREATED(111,  404), 
		EXISTE_CFE(112,  404)
		
		;


		int code;
		int httpCode;
		String message;
		String i18nKey;
		
		
		APIErrors(int code, int httpCode)
		{
			this.code = code;
			this.httpCode = httpCode;
			this.i18nKey = name();
		}
		
		public APIErrors withParams(Object... args){
			message = Messages.get(i18nKey, args);
			return this;
		}
		
		public int code()
		{
			return code;
		}
		public String message()
		{
			if (message==null)
				message = Messages.get(i18nKey);
			return message;
		}
		
		public int httpCode()
		{
			return httpCode;
		}
	}
	
	
	
	public static APIException raise(APIErrors error)
	{
		return new APIException(error);
	}
	
	public static APIException raise(APIErrors error, Throwable e)
	{
		return new APIException(error, e);
	}
	
	public static APIException raise(Throwable e)
	{
		return new APIException(APIErrors.WRAPPED_EXCEPTION, e);
	}
	
	
	
	public String getDetailMessage()
	{
		return detailMessage;
	}

	public APIException setDetailMessage(String detailMessage)
	{
		this.detailMessage += detailMessage;
		return this;
	}
	
	public APIException setDetailMessage(List<String> detailMessage)
	{
		int i=0;
		for (Iterator<String> iterator = detailMessage.iterator(); iterator.hasNext();)
		{
			String string = iterator.next();
			if (i==0)
				this.detailMessage += "[" + string;
			else
				this.detailMessage += "," + string;
			i++;
		}
		this.detailMessage += "]";
		return this;
	}

	public APIErrors getError()
	{
		return error;
	}

	public void setError(APIErrors error)
	{
		this.error = error;
	}
	
	@Override
	public String getMessage()
	{
		return super.getMessage() + (getDetailMessage()!=null?". "+getDetailMessage():"");
	}
}