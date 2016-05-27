package com.bluedot.efactura.impl;

import java.io.IOException;

import com.bluedot.efactura.CAEManager;
import com.bluedot.efactura.CFEController;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.ServiceController;
import com.bluedot.efactura.global.EFacturaException;

public class EFacturaFactoryImpl implements EFacturaFactory
{
	
	private static EFacturaFactory INSTANCE;
	
	private MODO modo;
	
	private synchronized static void createInstance() {
        if (INSTANCE == null) { 
            INSTANCE = new EFacturaFactoryImpl(MODO.NORMAL);
        }
    }

    @Override
	public MODO getModo() {
		return modo;
	}

	@Override
	public void setModo(MODO modo) {
		this.modo = modo;
	}

	public static EFacturaFactory getInstance() {
        if (INSTANCE == null) createInstance();
        return INSTANCE;
    }

	public EFacturaFactoryImpl(MODO modo) {
		super();
		this.modo = modo;
	}

	@Override
	public CAEManager getCAEManager() throws EFacturaException
	{
		try
		{
			return CAEManagerImpl.getInstance();
		} catch (IOException e)
		{
			e.printStackTrace();
			throw EFacturaException.raise(e);

		}
	}

	@Override
	public CFEController getCFEController() throws EFacturaException
	{
		return new CFEControllerImpl(modo);

	}

	@Override
	public ServiceController getServiceController() throws EFacturaException {
		return new ServiceControllerImpl();
	}

}
