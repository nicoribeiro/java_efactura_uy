package com.bluedot.efactura.model;

import java.util.Map;
import java.util.TreeMap;

public enum TipoDocumento {

	RUC(2,"RUC (Uruguay)"), 
	CI(3,"C.I. (Uruguay)"), 
	OTROS(4,"Otros"), 
	PASAPORTE(5,"Pasaporte"), 
	DNI(6,"DNI (Argentina, Brasil, Chile o Paraguay)");
	
	private int id;
	private String descripcion;
	
	private static Map<Integer, TipoDocumento> idMap = new TreeMap<Integer, TipoDocumento>();

	static {
		for (TipoDocumento tipoDoc : values()) {
			idMap.put(tipoDoc.id, tipoDoc);
		}
	}

	public static TipoDocumento fromInt(int i) {
		return idMap.get(i);
	}
	
	TipoDocumento(int id, String descripcion){
		this.id = id;
		this.descripcion = descripcion;
	}

	public int getId() {
		return id;
	}

	public String getDescripcion() {
		return descripcion;
	}
	
}
