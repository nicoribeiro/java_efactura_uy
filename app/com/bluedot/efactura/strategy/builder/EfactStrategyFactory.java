package com.bluedot.efactura.strategy.builder;

import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;

public interface EfactStrategyFactory {
	public EfactStrategy create(CFE cfe, Empresa empresa);
}
