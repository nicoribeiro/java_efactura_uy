package com.bluedot.efactura.strategy.builder;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;

public interface CFEBuiderInterface {
	
	void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException;

	void buildReceptor(JSONObject receptorJson) throws APIException;

	void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException;

	void buildIdDoc(boolean montosIncluyenIva, Integer formaPago, JSONObject idDocJson) throws APIException;

	void asignarId() throws APIException;
	
	void buildCAEData() throws APIException;

	void buildEmisor(Empresa empresaEmisora) throws APIException;
	
	void buildEmisor(JSONObject emisorJson) throws APIException;

	void buildReferencia(Empresa empresaEmisora, JSONArray referencia) throws APIException;

	void buildTimestampFirma(Long timestamp) throws APIException;

	CFE getCFE();

}
