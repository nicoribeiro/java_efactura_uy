package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.Titular;

public class TitularSerializer<T> extends JSONSerializer<Titular> {

	JSONSerializer<Pais> paisSerializer;
	
	public TitularSerializer(JSONSerializer<Pais> paisSerializer) {
		this.paisSerializer = paisSerializer;
	}
	
	@Override
	public JSONObject objectToJson(Titular titular, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject titularJson = new JSONObject();
		
		titularJson.put("id", titular.getId());
		
		if (!shrinkSerializarion) {
			titularJson.put("documento", titular.getDocumento());
			titularJson.put("email", titular.getEmail());
			titularJson.put("paisEmisorDocumento", titular.getPaisEmisorDocumento()!=null? paisSerializer.objectToJson(titular.getPaisEmisorDocumento(), true): JSONObject.NULL);
			titularJson.put("tipoDocumento", titular.getTipoDocumento());
			titularJson.put("tipoFacturacionElectronica", titular.getTipoFacturacionElectronica());
		}
		return titularJson;
	}

}
