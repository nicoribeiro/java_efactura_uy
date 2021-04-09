package com.bluedot.efactura.strategy.asignarFecha;

import java.util.Date;

public class EstrategiaFechaAhora implements EstrategiaAsignarFecha {

	@Override
	public Date getDate() {
		return new Date();
	}

}