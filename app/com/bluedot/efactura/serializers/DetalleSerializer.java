package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Detalle;

public class DetalleSerializer<T> extends JSONSerializer<Detalle> {

	@Override
	public JSONObject objectToJson(Detalle detalle, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject cfeJson = new JSONObject();
		//TODO revisar que la nomenclatura de los parametros es diferente en el serializer (aqui) que el formato del POST Documento. combianar a una comun
		
		cfeJson.put("id", detalle.getId());
		
		if (!shrinkSerializarion) {
			cfeJson.put("cantidad", detalle.getCantidad());
			cfeJson.put("nombreItem", detalle.getNombreItem());
			cfeJson.put("montoItem", detalle.getMontoItem());
			cfeJson.put("nroLinea", detalle.getNroLinea());
			cfeJson.put("precioUnitario", detalle.getPrecioUnitario());
			cfeJson.put("unidadMedida", detalle.getUnidadMedida());
		}
		return cfeJson;
	}

}
