package com.bluedot.efactura.microControllers.factory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryBuilder;

public class EfacturaMicroControllersFactoryBuilder extends MicroControllerFactoryBuilder {

	public EfacturaMicroControllersFactory getMicroControllersFactory() throws APIException {
		EfacturaMicroControllersFactory microControllersFactory = new EfacturaMicroControllersFactoryDefault();

		return microControllersFactory;

	}
}
