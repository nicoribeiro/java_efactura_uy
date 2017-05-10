package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Pais;

public class PaisSerializer<T> extends JSONSerializer<Pais>
{
	
	@Override
	public JSONObject objectToJson(Pais p) throws JSONException
	{
		
		JSONObject pais = new JSONObject();
		
		pais.put("id", p.getId());
		pais.put("codigo", p.getCodigo());
		pais.put("descripcion", p.getDescripcion());
		
		return pais;
	}

}
