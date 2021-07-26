package com.bluedot.efactura.strategy.numeracion;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CAE;

public interface EstrategiaNumeracion {
	
	long getId(CAE cae) throws APIException ;
	
}
