package com.bluedot.efactura.strategy.builder;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;

import dgi.classes.recepcion.ComplFiscalDataType;
import dgi.classes.recepcion.ComplFiscalType;

public class CFEBuilderCtaAjena extends CFEBuilderImpl implements CFEBuiderInterface {

	public CFEBuilderCtaAjena(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}
	
	@Override
	public void buildComplementoFiscal(JSONObject complementoFiscalJson) throws APIException {
		
		// Venta por cuenta ajena: todos los datos del mandante son obligatorios (zona del complemento fiscal del formato de DGI)
		if (complementoFiscalJson == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("ComplementoFiscal");

		ComplFiscalType complFiscalType = strategy.getComplementoFiscal();
		
		ComplFiscalDataType complFiscalDataType = new ComplFiscalDataType();
				
		String rucEmisor = obligatorio(complementoFiscalJson, "RUCEmisor");

		// Antes: int = null cuando faltaba -> NullPointerException
		int tipoDocMdte;
		try {
			tipoDocMdte = Integer.parseInt(obligatorio(complementoFiscalJson, "TipoDocMdte"));
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDocMdte", complementoFiscalJson.get("TipoDocMdte"));
		}

		String pais = obligatorio(complementoFiscalJson, "Pais");

		String docMdte = obligatorio(complementoFiscalJson, "DocMdte");

		String nombreMdte = obligatorio(complementoFiscalJson, "NombreMdte");
		
		complFiscalDataType.setRUCEmisor(rucEmisor);
		complFiscalDataType.setTipoDocMdte(tipoDocMdte);
		complFiscalDataType.setPais(pais);
		complFiscalDataType.setDocMdte(docMdte);
		complFiscalDataType.setNombreMdte(nombreMdte);
		
		complFiscalType.setComplFiscalData(complFiscalDataType);
		
	}

	private static String obligatorio(JSONObject json, String campo) throws APIException {
		if (!json.has(campo) || json.isNull(campo) || json.get(campo).toString().trim().isEmpty())
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("ComplementoFiscal." + campo);
		return json.get(campo).toString().trim();
	}

}
