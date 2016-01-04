package com.bluedot.efactura.impl;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EfacturaResult;
import com.bluedot.efactura.ServiceController;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.soap.recepcion.Data;

public class ServiceControllerImpl implements ServiceController {

	RecepcionService service = null;
	
	@Override
	public EfacturaResult register(EFact efactura) throws EFacturaException {
		
		RecepcionService service = getRecepcionService();

		// Call the service
		Data response = service.sendCFE(efactura);

		return new EfacturaResult(response.getXmlData());
		
	}

	private RecepcionService getRecepcionService() {
		if (service==null)
			service = new RecepcionServiceImpl();
		return service;
	}

	@Override
	public EfacturaResult poll() throws EFacturaException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public EfacturaResult cancel() throws EFacturaException {
		// TODO Auto-generated method stub
		return null;
	}

}
