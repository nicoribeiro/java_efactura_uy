package com.bluedot.efactura.serializers;

import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;

public class CAESerializer<T> extends JSONSerializer<CAE> {

	JSONSerializer<Empresa> empresaSerializer;
	
	public CAESerializer(JSONSerializer<Empresa> empresaSerializer) {
		this.empresaSerializer = empresaSerializer;
	}
	
	@Override
	public JSONObject objectToJson(CAE cae, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject caeJson = new JSONObject();
		
		caeJson.put("id", cae.getId());
		
		if (!shrinkSerializarion) {
			caeJson.put("empresa", cae.getEmpresa()!=null? empresaSerializer.objectToJson(cae.getEmpresa(), true): JSONObject.NULL);
			caeJson.put("nro", cae.getNro());
			caeJson.put("serie", cae.getSerie());
			caeJson.put("inicial", cae.getInicial());
			caeJson.put("fin", cae.getFin());
			caeJson.put("fechaVencimiento", cae.getFechaVencimiento()!=null? sdf.format(cae.getFechaVencimiento()): JSONObject.NULL);
			caeJson.put("siguiente", cae.getSiguiente());
			caeJson.put("fechaAnulado", cae.getFechaAnulado()!=null? sdf.format(cae.getFechaAnulado()): JSONObject.NULL);
		}
		return caeJson;
	}

}
