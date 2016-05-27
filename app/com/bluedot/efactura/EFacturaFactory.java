package com.bluedot.efactura;

import com.bluedot.efactura.global.EFacturaException;

public interface EFacturaFactory
{

	public enum MODO {
		NORMAL, CONTINGENCIA
	}
	
	CAEManager getCAEManager() throws EFacturaException;
	
	CFEController getCFEController() throws EFacturaException;
	
	ServiceController getServiceController() throws EFacturaException;

	void setModo(MODO modo);

	MODO getModo();
	
	
	
}
