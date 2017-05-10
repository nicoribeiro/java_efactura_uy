package com.bluedot.efactura.microControllers.implementation;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;

public class MicroControllerDefault {

	protected Empresa empresa;
	
	public MicroControllerDefault(Empresa empresa) {
		this.empresa = empresa;
	}

}
