package com.bluedot.efactura.impl;

import java.io.IOException;

import com.bluedot.efactura.CAEManager;
import com.bluedot.efactura.CFEController;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaFactory;

public class EFacturaFactoryImpl implements EFacturaFactory
{

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
	public CFEController getEFAController() throws EFacturaException
	{
		return new CFEControllerImpl();

	}

}
