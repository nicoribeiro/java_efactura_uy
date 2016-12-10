package com.bluedot.efactura.strategy.builder;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;

public interface CFEBuilder {
	
	void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException;

	void buildReceptor(JSONObject receptorJson) throws APIException;

	void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException;

	void buildIdDoc(boolean montosIncluyenIva, int formaPago) throws APIException;

	void asignarId() throws APIException;
	
	void buildCAEData() throws APIException;

	void buildEmisor(Empresa empresaEmisora) throws APIException;

	void buildReferencia(Empresa empresaEmisora, JSONObject referencia) throws APIException;

	void buildTimestampFirma() throws APIException;

	CFE getCFE();

}
