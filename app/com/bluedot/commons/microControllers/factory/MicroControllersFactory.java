package com.bluedot.commons.microControllers.factory;

import com.bluedot.commons.microControllers.interfaces.AccountMicroController;

public interface MicroControllersFactory
{	
	AccountMicroController getAccountController();

}
