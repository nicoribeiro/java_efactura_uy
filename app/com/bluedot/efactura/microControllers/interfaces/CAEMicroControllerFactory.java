package com.bluedot.efactura.microControllers.interfaces;

import com.bluedot.efactura.model.Empresa;

public interface CAEMicroControllerFactory {
	public CAEMicroController create(Empresa empresa);
}
