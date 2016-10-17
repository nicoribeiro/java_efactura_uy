package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;

public class CFESerializer<T> extends JSONSerializer<CFE> {
	
	JSONSerializer<Empresa> empresaSerializer;
	JSONSerializer<Detalle> detalleSerializer;
	
	public CFESerializer(JSONSerializer<Empresa> empresaSerializer, JSONSerializer<Detalle> detalleSerializer) {
		this.empresaSerializer = empresaSerializer;
		this.detalleSerializer = detalleSerializer;
	}

	@Override
	public JSONObject objectToJson(CFE cfe) throws JSONException
	{
		
		JSONObject cfeJson = new JSONObject();
		//TODO revisar que la nomenclatura de los parametros es diferente en el serializer (aqui) que el formato del POST Documento. combianar a una comun
		cfeJson.put("id", cfe.getId());
		cfeJson.put("nro", cfe.getNro());
		cfeJson.put("serie", cfe.getSerie());
		cfeJson.put("emresaReceptora", cfe.getEmpresaReceptora()!=null? empresaSerializer.objectToJson(cfe.getEmpresaReceptora()): JSONObject.NULL);
		cfeJson.put("emresaEmisora", cfe.getEmpresaEmisora()!=null? empresaSerializer.objectToJson(cfe.getEmpresaEmisora()): JSONObject.NULL);
		cfeJson.put("detalle", cfe.getDetalle()!=null? detalleSerializer.objectToJson(cfe.getDetalle()): JSONObject.NULL);
		cfeJson.put("xml", cfe.getXml());
		cfeJson.put("estado", cfe.getEstado());
		
		return cfeJson;
	}

}
