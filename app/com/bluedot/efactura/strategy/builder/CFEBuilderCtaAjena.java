package com.bluedot.efactura.strategy.builder;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;

import dgi.classes.recepcion.ComplFiscalDataType;
import dgi.classes.recepcion.ComplFiscalType;

public class CFEBuilderCtaAjena extends CFEBuilderImpl implements CFEBuiderInterface {

	public CFEBuilderCtaAjena(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}
	
	@Override
	public void buildComplementoFiscal(JSONObject complementoFiscalJson) throws APIException {
		
		ComplFiscalType complFiscalType = strategy.getComplementoFiscal();
		
		ComplFiscalDataType complFiscalDataType = new ComplFiscalDataType();
				
		String rucEmisor = complementoFiscalJson.has("RUCEmisor") ? complementoFiscalJson.getString("RUCEmisor") : null;

		int tipoDocMdte = complementoFiscalJson.has("TipoDocMdte") ? complementoFiscalJson.getInt("TipoDocMdte") : null;

		String pais = complementoFiscalJson.has("Pais") ? complementoFiscalJson.getString("Pais") : null;

		String docMdte = complementoFiscalJson.has("DocMdte") ? complementoFiscalJson.getString("DocMdte") : null;

		String nombreMdte = complementoFiscalJson.has("NombreMdte") ? complementoFiscalJson.getString("NombreMdte") : null;
		
		complFiscalDataType.setRUCEmisor(rucEmisor);
		complFiscalDataType.setTipoDocMdte(tipoDocMdte);
		complFiscalDataType.setPais(pais);
		complFiscalDataType.setDocMdte(docMdte);
		complFiscalDataType.setNombreMdte(nombreMdte);
		
		complFiscalType.setComplFiscalData(complFiscalDataType);
		
	}

}
