package com.bluedot.efactura.microControllers.factory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryDefault;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.microControllers.implementation.CAEMicroControllerDefault;
import com.bluedot.efactura.microControllers.implementation.CFEMicroControllerDefault;
import com.bluedot.efactura.microControllers.implementation.IntercambioMicroControllerDefault;
import com.bluedot.efactura.microControllers.implementation.ServiceMicroControllerDefault;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.services.impl.ConsultasServiceImpl;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

public class EfacturaMicroControllersFactoryDefault extends MicroControllerFactoryDefault implements EfacturaMicroControllersFactory
{

	private static MODO_SISTEMA modo;
	
	static {
		modo = MODO_SISTEMA.NORMAL;
	}
	
	
	public MODO_SISTEMA getModo() {
		return modo;
	}

	@Override
	public synchronized void setModo(MODO_SISTEMA modo) {
		EfacturaMicroControllersFactoryDefault.modo = modo;
	}

	@Override
	public CFEMicroController getCFEMicroController(Empresa empresa) throws APIException
	{
		return new CFEMicroControllerDefault(modo, empresa, getCAEMicroController(empresa));

	}

	@Override
	public ServiceMicroController getServiceMicroController(Empresa empresa) throws APIException {
		return new ServiceMicroControllerDefault(new RecepcionServiceImpl(), empresa, getCAEMicroController(empresa), new ConsultasServiceImpl(), new IntercambioMicroControllerDefault(getCFEMicroController(empresa)));
	}

	@Override
	public CAEMicroController getCAEMicroController(Empresa empresa) throws APIException {
		return new CAEMicroControllerDefault(empresa);
	}


}
