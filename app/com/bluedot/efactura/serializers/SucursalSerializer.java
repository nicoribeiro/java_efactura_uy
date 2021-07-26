package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Sucursal;

public class SucursalSerializer<T> extends JSONSerializer<Sucursal> {

	@Override
	public JSONObject objectToJson(Sucursal p, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject sucursal = new JSONObject();
		
		sucursal.put("id", p.getId());
		
		if (!shrinkSerializarion) {
			sucursal.put("DomFiscal", p.getDomicilioFiscal());
			sucursal.put("Ciudad", p.getCiudad());
			sucursal.put("CdgDGISucur", p.getCodigoSucursal());
			sucursal.put("Telefono", p.getTelefono());
			sucursal.put("CodigoPostal", p.getCodigoPostal());
			sucursal.put("Departamento", p.getDepartamento());
		}
		
		return sucursal;
	}

}
