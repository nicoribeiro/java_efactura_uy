package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;

public interface CFEBuilderResguardoFactory {
	public CFEBuilderResguardo create(CFEStrategy strategy, CAEMicroController caeMicroController);
}
