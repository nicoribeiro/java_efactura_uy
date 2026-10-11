package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sucursal;
import com.bluedot.efactura.model.Titular;

public class EmpresaSerializer<T> extends JSONSerializer<Empresa> {

	JSONSerializer<Sucursal> sucursalSerializer;
	
	public EmpresaSerializer(JSONSerializer<Sucursal> sucursalSerializer) {
		this.sucursalSerializer = sucursalSerializer;
	}
	
	@Override
	public JSONObject objectToJson(Empresa p, boolean shrinkSerializarion) throws JSONException
	{
		
		JSONObject empresa = new JSONObject();
		
		empresa.put("id", p.getId());
		// rut y razon tambien en la version reducida (CFE, sobres, CAE, reportes) para no tener que buscar la empresa aparte
		empresa.put("rut", p.getRut());
		empresa.put("razon", p.getRazon());

		if (!shrinkSerializarion) {
			empresa.put("nombreComercial", p.getNombreComercial());
			empresa.put("mailRecepcion", p.getMailRecepcion());
			empresa.put("hostRecepcion", p.getHostRecepcion());
			empresa.put("userRecepcion", p.getUserRecepcion());
			empresa.put("puertoRecepcion", p.getPuertoRecepcion());
			empresa.put("emisorElectronico", p.isEmisorElectronico());
			empresa.put("mailNotificaciones", p.getMailNotificaciones());
			empresa.put("fromEnvio", p.getFromEnvio());
			empresa.put("offsetMail", p.getOffsetMail());
			empresa.put("paginaWeb", p.getPaginaWeb());
			empresa.put("resolucion", p.getResolucion());
			empresa.put("sucursales", sucursalSerializer.objectToJson(p.getSucursales(), false));
			// Solo las fechas de validez: nunca el certificado ni la clave privada
			if (p.getFirmaDigital() != null) {
				JSONObject firma = new JSONObject();
				firma.put("validaDesde", p.getFirmaDigital().getValidaDesde() != null ? sdf.format(p.getFirmaDigital().getValidaDesde()) : JSONObject.NULL);
				firma.put("validaHasta", p.getFirmaDigital().getValidaHasta() != null ? sdf.format(p.getFirmaDigital().getValidaHasta()) : JSONObject.NULL);
				empresa.put("firmaDigital", firma);
			} else
				empresa.put("firmaDigital", JSONObject.NULL);
		}
		
		return empresa;
	}

}
