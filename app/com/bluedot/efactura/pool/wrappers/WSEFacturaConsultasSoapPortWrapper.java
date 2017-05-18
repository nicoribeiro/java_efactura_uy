package com.bluedot.efactura.pool.wrappers;

import dgi.soap.consultas.WSEFacturaConsultasSoapPort;

public class WSEFacturaConsultasSoapPortWrapper {

	private WSEFacturaConsultasSoapPort port;

	public WSEFacturaConsultasSoapPort getPort() {
		return port;
	}

	public void setPort(WSEFacturaConsultasSoapPort port) {
		this.port = port;
	}

	public WSEFacturaConsultasSoapPortWrapper(WSEFacturaConsultasSoapPort port) {
		super();
		this.port = port;
	}
	
}
