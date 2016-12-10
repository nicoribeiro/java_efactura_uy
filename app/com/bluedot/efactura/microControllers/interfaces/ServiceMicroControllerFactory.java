package com.bluedot.efactura.microControllers.interfaces;

import com.bluedot.efactura.model.Empresa;

public interface ServiceMicroControllerFactory {
	public ServiceMicroController create(Empresa empresa);
}
