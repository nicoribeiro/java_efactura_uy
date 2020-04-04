package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Empresa;

public class EmpresaSerializer<T> extends JSONSerializer<Empresa> {

	@Override
	public JSONObject objectToJson(Empresa p, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject empresa = new JSONObject();
		
		empresa.put("id", p.getId());
		
		if (!shrinkSerializarion) {
			empresa.put("rut", p.getRut());
			empresa.put("nombreComercial", p.getNombreComercial());
			empresa.put("razon", p.getRazon());
			empresa.put("direccion", p.getDireccion());
			empresa.put("localidad", p.getLocalidad());
			empresa.put("codigoSucursal", p.getCodigoSucursal());
			empresa.put("mailRecepcion", p.getMailRecepcion());
			empresa.put("hostRecepcion", p.getHostRecepcion());
			empresa.put("userRecepcion", p.getUserRecepcion());
			empresa.put("puertoRecepcion", p.getPuertoRecepcion());
			empresa.put("emisorElectronico", p.isEmisorElectronico());
			empresa.put("mailNotificaciones", p.getMailNotificaciones());
			empresa.put("fromEnvio", p.getFromEnvio());
			empresa.put("offsetMail", p.getOffsetMail());
			empresa.put("paginaWeb", p.getPaginaWeb());
			empresa.put("telefono", p.getTelefono());
			empresa.put("codigoPostal", p.getCodigoPostal());
			empresa.put("resolucion", p.getResolucion());
		}
		
		return empresa;
	}

}
