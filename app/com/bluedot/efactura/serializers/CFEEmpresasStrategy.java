package com.bluedot.efactura.serializers;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public interface CFEEmpresasStrategy {

	JSONObject getEncabezado(CFEEmpresasType cfe, Empresa empresaReceptora);

	JSONArray getDetalle(CFEEmpresasType cfe);
	
	JSONArray getReferencia(CFEEmpresasType cfe);

	long getTimestampFirma(CFEEmpresasType cfe);

	JSONObject getCompFiscal(CFEEmpresasType cfe);

	boolean hayCompFiscal(CFEEmpresasType cfe);
	
	JSONObject getTotales(CFEEmpresasType cfe);

}
