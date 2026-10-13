package com.bluedot.efactura.model;

public enum MotivoRechazoReporte {

	R01("Formato del archivo no es el indicado"),

	R02("No coincide RUC en Reporte, Certificado o envío"),

	R03("Firma o Certificado electrónicos no son válidos"),

	R04("No cumple validaciones según Formato de Reporte"),

	R05("La secuencia indicada en el reporte no es correcta"),
	R06("Fecha recepción no puede ser menor a fecha resumen");

	private String motivo;

	MotivoRechazoReporte(String motivo){
		this.motivo=motivo;
	}

	public String getMotivo() {
		return motivo;
	}

}
