package com.bluedot.efactura.strategy.numeracion;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.CAE;

public class EstrategiaNumeracionSiguiente implements EstrategiaNumeracion {

	@Override
	public long getId(CAE cae) throws APIException {
		if (cae.getSiguiente()  > cae.getFin())
			throw APIException.raise(APIErrors.CAE_NOT_AVAILABLE_ID).withParams(cae.getNro(), cae.getTipo());

		long siguiente = cae.getSiguiente();
		cae.setSiguiente(siguiente+1);
		cae.update();
		return siguiente;
	}

}
