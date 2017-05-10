package com.bluedot.efactura.model;

public enum MotivoRechazoCFE {

	E01("Tipo y No de CFE ya fue reportado como anulado"),
	
	E02("Tipo y No de CFE ya existe en los registros"),
	
	E03("Tipo y No de CFE no se corresponden con el CAE"),
	
	E04("Firma electrónica no es válida"),
	
	E05("No cumple validaciones (*) de Formato comprobantes"),
	
	E07("Fecha Firma de CFE no se corresponde con fecha CAE"),
	
	E08("No coinciden RUC emisor de CFE y Complemento Fiscal"),
	
	E20("Orden de compra vencida"),
	
	E21("Mercadería en mal estado"),
	
	E22("Proveedor inhabilitado por organismo de contralor"),
	
	E23("Contraprestación no recibida"),
	
	E24("Diferencia precios y/o descuentos"),
	
	E25("Factura con error cálculos"),
	
	E26("Diferencia con plazos");
	
	private String motivo;

	MotivoRechazoCFE(String motivo){
		this.motivo=motivo;
	}

	public String getMotivo() {
		return motivo;
	}
}
