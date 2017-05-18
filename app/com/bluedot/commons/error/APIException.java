package com.bluedot.commons.error;


import java.util.Iterator;
import java.util.List;

import org.json.JSONObject;

import play.i18n.Messages;




public class APIException extends Throwable
{

	private APIException(APIErrors error, Throwable cause) {
		super("Wrraped Exception", cause);
		this.error = error;
		this.log = true;
		
	}
	
	private APIException(APIErrors error) {
		super(error.message);
		this.error = error;
		this.log = true;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2194392490942571005L;
	
	private String detailMessage= "";
	private APIErrors error;
	private boolean log;
	
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
		BAD_PARAMETER_VALUE(1, 200, true), 
		MISSING_PARAMETER(2, 200, true), 
		NOT_SUPPORTED(3,200, true), 
		INTERNAL_SERVER_ERROR(4, 200, true),
		NOT_IMPLEMENTED(5,  200, true), 
		UNAUTHORIZED(6,  200, true),
		BAD_JSON(7,  200, true),
		
		/*
		 * This error is used when the APIexception is acting as a wrapper for another exception.
		 */
		WRAPPED_EXCEPTION(8, 200, true), 
		 
		
		
		/*
		 * Commons Errors
		 */
		ACCOUNT_NOT_FOUND(9,  200, true),
		USER_NOT_FOUND(10,  200, true),
		ALERT_NOT_FOUND(11,  200, true),
		CREDENTIAL_NOT_FOUND(12,  200, true), 
		SESSION_NOT_FOUND(13,  200, true),
		INVALID_VALIDATION_KEY(14,  200, true),
		NO_AUTH_METHOD_DEFINED(15,  200, true),
		HAZELCAST(16,  200, true),
		USER_NOT_PART_OF_ACCOUNT(17,  200, true),
		USER_ALREADY_EXISTS(18,  200, true),
		EMAIL_EXISTS(19,  200, true), 
		PHONE_EXISTS(20,  200, true),
		SETTING_SCHEMA_ERROR(21,  200, true),
		
		
		
		 
		
		
		/*
		 * Exceptions de facturacion electronica
		 */
		CAE_DATA_NOT_FOUND(100, 200, true),
		CAE_NOT_AVAILABLE_ID(101,200, true), 
		MALFORMED_CFE(102,200, true), 
		PAIS_NO_ENCONTRADO(103, 200, true),
		EMPRESA_NO_ENCONTRADA(104, 200, true),
		ERROR_COMUNICACION_DGI(105,  200, false), 
		REPORTE_DIARIO_NO_ENCONTRADO(106,  200, true),
		NO_CFE_CREATED(107,  200, true),
		CFE_NO_ENCONTRADO(108,  200, true),
		SOBRE_NO_ENCONTRADO(109,  200, true), 
		UI_NO_ENCONTRADA(110,  200, true), 
		NO_REPORT_CREATED(111,  200, true), 
		EXISTE_CFE(112,  200, true), 
		SOBRE_RECHAZADO(113, 200, true), 
		CFE_YA_FUE_ANULADO(114, 200, true), 
		CFE_NO_SE_PUEDE_ANULAR(115, 200, true), 
		HAY_CFE_SIN_RESPUESTA(116, 200, true),
		FALTA_TIPO_CAMBIO(117,  404, true), 
		TEMPRANO_PARA_GENERAR_REPORTE(118,  404, true), 
		SOBRE_YA_ENVIADO(119,  404, true) 
		;


		int code;
		int httpCode;
		String message;
		String i18nKey;
		boolean log;
		
		
		APIErrors(int code, int httpCode, boolean log)
		{
			this.code = code;
			this.httpCode = httpCode;
			this.log = log;
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
		return  getJSONObject().toString();
	}

	public boolean isLog() {
		return log;
	}

	public void setLog(boolean log) {
		this.log = log;
	}
	
	public  JSONObject getJSONObject()
	{
		JSONObject jsonError = new JSONObject();
		jsonError.put("result_code", error.code);
		jsonError.put("result_message", error.name());
		jsonError.put("result_detail", getDetailMessage());
		
		return jsonError;
	}
}