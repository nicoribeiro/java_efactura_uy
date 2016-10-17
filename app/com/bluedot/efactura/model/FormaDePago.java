package com.bluedot.efactura.model;

import java.util.Map;
import java.util.TreeMap;

public enum FormaDePago {

	CONTADO(1), CREDITO(2);
	
	public int value;

	private static Map<Integer, FormaDePago> idMap = new TreeMap<Integer, FormaDePago>();

	static {
		for (FormaDePago formaDePago : values()) {
			idMap.put(formaDePago.value, formaDePago);
		}
	}

	public static FormaDePago fromInt(int i) {
		return idMap.get(i);
	}

	FormaDePago(int value) {
		this.value = value;
	}
}
