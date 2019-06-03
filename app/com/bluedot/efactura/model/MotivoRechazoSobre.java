package com.bluedot.efactura.model;

public enum MotivoRechazoSobre {

	S01("Formato del archivo no es el indicado"),
	
	S02("No coincide RUC de Sobre, Certificado, envío o CFE"),
	
	S03("Certificado electrónico no es válido"),
	
	S04("No cumple validaciones según Formato de sobre"),
	
	S05("No coinciden cantidad CFE de carátula y contenido"),
	
	S06("No coinciden certificado de sobre y comprobantes"),
	
	S07("Sobre enviado supera el tamaño máximo admitido"),
	
	S08("Sobre enviado ya existe en los registros de DGI");
	
	private String motivo;

	MotivoRechazoSobre(String motivo){
		this.motivo=motivo;
	}

	public String getMotivo() {
		return motivo;
	}
}
