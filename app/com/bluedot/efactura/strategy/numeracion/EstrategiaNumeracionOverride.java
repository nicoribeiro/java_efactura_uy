package com.bluedot.efactura.strategy.numeracion;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CAE;

public class EstrategiaNumeracionOverride implements EstrategiaNumeracion {

	private long id;
	
	public EstrategiaNumeracionOverride(long id) {
		super();
		this.id = id;
	}

	@Override
	public long getId(CAE cae) throws APIException {
		return id;
	}

}