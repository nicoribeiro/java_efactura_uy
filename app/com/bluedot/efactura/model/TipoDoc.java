package com.bluedot.efactura.model;

import java.util.Map;
import java.util.TreeMap;

public enum TipoDoc {

	eTicket(101, "eTicket"),

	Nota_de_Credito_de_eTicket(102, "Nota de Credito de eTicket"),

	Nota_de_Debito_de_eTicket(103, "Nota de Debito de eTicket"),

	eFactura(111, "eFactura"),

	Nota_de_Credito_de_eFactura(112, "Nota de Credito de eFactura"),

	Nota_de_Debito_de_eFactura(113, "Nota de Debito de eFactura"),

	eFactura_Exportacion(121, "eFactura Exportacion"),

	Nota_de_Credito_de_eFactura_Exportacion(122, "Nota de Credito de eFactura Exportacion"),

	Nota_de_Debito_de_eFactura_Exportacion(123, "Nota de Debito de eFactura Exportacion"),

	eRemito_de_Exportacion(124, "eRemito de Exportacion"),

	eTicket_Venta_por_Cuenta_Ajena(131, "eTicket Venta por Cuenta Ajena"),

	Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena(132, "Nota de Credito de eTicket Venta por Cuenta Ajena"),

	Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena(133, "Nota de Debito de eTicket Venta por Cuenta Ajena"),

	eFactura_Venta_por_Cuenta_Ajena(141, "eFactura Venta por Cuenta Ajena"),

	Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena(142, "Nota de Credito de eFactura Venta por Cuenta Ajena"),

	Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena(143, "Nota de Debito de eFactura Venta por Cuenta Ajena"),

	eRemito(181, "eRemito"),

	eResguardo(182, "eResguardo"),

	eTicket_Contingencia(201, "eTicket Contingencia"),

	Nota_de_Credito_de_eTicket_Contingencia(202, "Nota de Credito de eTicket Contingencia"),

	Nota_de_Debito_de_eTicket_Contingencia(203, "Nota de Debito de eTicket Contingencia"),

	eFactura_Contingencia(211, "eFactura Contingencia"),

	Nota_de_Credito_de_eFactura_Contingencia(212, "Nota de Credito de eFactura Contingencia"),

	Nota_de_Debito_de_eFactura_Contingencia(213, "Nota de Debito de eFactura Contingencia"),

	eFactura_Exportacion_Contingencia(221, "eFactura Exportacion Contingencia"),

	Nota_de_Credito_de_eFactura_Exportacion_Contingencia(222, "Nota de Credito de eFactura Exportacion Contingencia"),

	Nota_de_Debito_de_eFactura_Exportacion_Contingencia(223, "Nota de Debito de eFactura Exportacion Contingencia"),

	eRemito_de_Exportacion_Contingencia(224, "eRemito de Exportacion Contingencia"),

	eTicket_Venta_por_Cuenta_Ajena_Contingencia(231, "eTicket Venta por Cuenta Ajena Contingencia"),

	Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(232,
			"Nota de Credito de eTicket Venta por Cuenta Ajena Contingencia"),

	Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia(233,
			"Nota de Debito de eTicket Venta por Cuenta Ajena Contingencia"),

	eFactura_Venta_por_Cuenta_Ajena_Contingencia(241, "eFactura Venta por Cuenta Ajena Contingencia"),

	Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(242,
			"Nota de Credito de eFactura Venta por Cuenta Ajena Contingencia"),

	Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia(243,
			"Nota de Debito de eFactura Venta por Cuenta Ajena Contingencia"),

	eRemito_Contingencia(281, "eRemito Contingencia"),

	eResguardo_Contingencia(282, "eResguardo Contingencia");

	public int value;
	public String friendlyName;

	private static Map<Integer, TipoDoc> idMap = new TreeMap<Integer, TipoDoc>();

	static {
		for (TipoDoc tipoDoc : values()) {
			idMap.put(tipoDoc.value, tipoDoc);
		}
	}

	public static TipoDoc fromInt(int i) {
		return idMap.get(i);
	}

	TipoDoc(int value, String friendlyName) {
		this.value = value;
		this.friendlyName = friendlyName;
	}

}