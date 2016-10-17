package com.bluedot.commons.microControllers.factory;

import com.bluedot.commons.microControllers.implementation.AccountMicroControllerDefault;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController;

public class MicroControllerFactoryDefault implements MicroControllersFactory
{

	@Override
	public AccountMicroController getAccountController()
	{
		return new AccountMicroControllerDefault();
	}

}
