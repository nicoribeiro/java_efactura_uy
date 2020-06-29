package com.bluedot.efactura.serializers;

import org.json.JSONObject;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.recepcion.ComplFiscalDataType;
import dgi.classes.recepcion.ComplFiscalType;


public abstract class CommonStrategy implements CFEEmpresasStrategy{

	public CommonStrategy() {
		super();
	}

	public abstract ComplFiscalType getComplementoFiscalType(CFEEmpresasType cfe);
	
	@Override
	public JSONObject getCompFiscal(CFEEmpresasType cfe) {
		
		JSONObject compFiscal = new JSONObject();
		
		ComplFiscalDataType complFiscalDataType = getComplementoFiscalType(cfe).getComplFiscalData();
		
		compFiscal.put("TipoDocMdte", complFiscalDataType.getTipoDocMdte());
		compFiscal.put("DocMdte", complFiscalDataType.getDocMdte());
		compFiscal.put("NombreMdte", complFiscalDataType.getNombreMdte());
		compFiscal.put("Pais", complFiscalDataType.getPais());
		compFiscal.put("RUCEmisor", complFiscalDataType.getRUCEmisor());
		
		return compFiscal;
	}

}