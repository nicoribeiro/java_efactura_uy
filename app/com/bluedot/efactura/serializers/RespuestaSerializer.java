package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Respuesta;

public class RespuestaSerializer<T> extends JSONSerializer<Respuesta> {

	@Override
	public JSONObject objectToJson(Respuesta respuesta, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject respuestaJson = new JSONObject();
		
		respuestaJson.put("id", respuesta.getId());
		
		if (!shrinkSerializarion) {
			respuestaJson.put("nombreArchivo", respuesta.getNombreArchivo());
			respuestaJson.put("payload", respuesta.getPayload());
		}
		return respuestaJson;
	}

}
