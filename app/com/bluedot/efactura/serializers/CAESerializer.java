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
			caeJson.put("tipo", cae.getTipo()!=null? cae.getTipo().name(): JSONObject.NULL);
			caeJson.put("idTipoDoc", cae.getTipo()!=null? cae.getTipo().value: JSONObject.NULL);
			caeJson.put("nro", cae.getNro());
			caeJson.put("serie", cae.getSerie());
			caeJson.put("inicial", cae.getInicial());
			caeJson.put("fin", cae.getFin());
			caeJson.put("fechaVencimiento", cae.getFechaVencimiento()!=null? sdf.format(cae.getFechaVencimiento()): JSONObject.NULL);
			caeJson.put("siguiente", cae.getSiguiente());
			caeJson.put("fechaAnulado", cae.getFechaAnulado()!=null? sdf.format(cae.getFechaAnulado()): JSONObject.NULL);
			// Uso del rango: siguiente es el proximo numero a asignar
			long total = cae.getFin() - cae.getInicial() + 1;
			long usados = Math.max(0, Math.min(cae.getSiguiente() - cae.getInicial(), total));
			caeJson.put("total", total);
			caeJson.put("usados", usados);
			caeJson.put("disponibles", total - usados);
			caeJson.put("porcentajeUso", total > 0 ? Math.round(usados * 10000.0 / total) / 100.0 : 0);
		}
		return caeJson;
	}

}
