package com.bluedot.efactura.microControllers.interfaces;

import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.model.Empresa;

public interface CFEMicroControllerFactory {
	public CFEMicroController create(MODO_SISTEMA modo, Empresa empresa);
}
