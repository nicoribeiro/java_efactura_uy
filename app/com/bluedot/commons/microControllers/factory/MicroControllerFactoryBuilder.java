package com.bluedot.commons.microControllers.factory;

import com.bluedot.commons.error.APIException;

public class MicroControllerFactoryBuilder {

	public MicroControllersFactory getMicroControllersFactory() throws APIException {
		MicroControllersFactory microControllersFactory = new MicroControllerFactoryDefault();

		return microControllersFactory;
	}
}
