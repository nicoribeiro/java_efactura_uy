package com.bluedot.efactura.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;

import play.db.jpa.JPAApi;

public class ReporteDiarioSerializer<T> extends JSONSerializer<ReporteDiario> {

	JSONSerializer<Empresa> empresaSerializer;
	
	public ReporteDiarioSerializer(JSONSerializer<Empresa> empresaSerializer) {
		this.empresaSerializer = empresaSerializer;
	}
	
	
	@Override
	public JSONObject objectToJson(JPAApi jpaApi, ReporteDiario reporteDiario) throws JSONException {
		JSONObject reporte = new JSONObject();
		
		reporte.put("secuencial", reporteDiario.getSecuencial());
		reporte.put("empresa", empresaSerializer.objectToJson(jpaApi, reporteDiario.getEmpresa()));
		reporte.put("fecha",reporteDiario.getFecha());
		
		return reporte;
	}

}
