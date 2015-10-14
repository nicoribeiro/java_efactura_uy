package com.bluedot.efactura;

public interface EFacturaFactory
{

	CAEManager getCAEManager() throws EFacturaException;
	
	CFEController getCFEController() throws EFacturaException;
	
	ServiceController getServiceController() throws EFacturaException;
	
}
