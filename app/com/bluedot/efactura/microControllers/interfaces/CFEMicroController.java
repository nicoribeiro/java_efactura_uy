package com.bluedot.efactura.microControllers.interfaces;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.TipoDoc;

public interface CFEMicroController
{
	/**
	 * Crea una nuevo CFE a partir de un objeto JSON.
	 * @param tipo el tipo de CFE
	 * @param jsonObject el objecto JSON que define al CFE
	 * @return un nuevo CFE con los datos del objecto JSON
	 * @throws APIException cuando hay un error
	 */
	public CFE create(TipoDoc tipo, JSONObject jsonObject, boolean esCfeEmitido) throws APIException;
	
	
}