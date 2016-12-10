package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;

public interface CFEBuilderImplFactory {
	public CFEBuilderImpl create(CFEStrategy strategy, CAEMicroController caeMicroController);
}
