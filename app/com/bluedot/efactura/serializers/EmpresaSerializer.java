package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Empresa;

public class EmpresaSerializer<T> extends JSONSerializer<Empresa> {

	@Override
	public JSONObject objectToJson(Empresa p) throws JSONException
	{
		
		JSONObject pais = new JSONObject();
		
		pais.put("id", p.getId());
		pais.put("codigo", p.getRut());
		pais.put("nombre", p.getNombreComercial());
		pais.put("razon", p.getRazon());
		
		return pais;
	}

}
