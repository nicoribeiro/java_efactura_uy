package com.bluedot.efactura;

public interface EFacturaFactory
{

	CAEManager getCAEManager() throws EFacturaException;
	
	CFEController getEFAController() throws EFacturaException;
	
}
