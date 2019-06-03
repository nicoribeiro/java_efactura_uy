package com.bluedot.efactura.serializers;

import org.json.JSONArray;
import org.json.JSONObject;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public interface CFEEmpresasStrategy {

	JSONObject getEncabezado(CFEEmpresasType cfe);

	JSONArray getDetalle(CFEEmpresasType cfe);
	
	JSONArray getReferencia(CFEEmpresasType cfe);

	long getTimestampFirma(CFEEmpresasType cfe);

}
