package com.bluedot.efactura.microControllers.factory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.microControllers.factory.MicroControllersFactory;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.Empresa;

public interface EfacturaMicroControllersFactory extends MicroControllersFactory{

	CFEMicroController getCFEMicroController(Empresa empresa) throws APIException;
	
	ServiceMicroController getServiceMicroController(Empresa empresa) throws APIException;
	
	CAEMicroController getCAEMicroController(Empresa empresa) throws APIException;
	
	void setModo(MODO_SISTEMA modo);

	MODO_SISTEMA getModo();
	
}
