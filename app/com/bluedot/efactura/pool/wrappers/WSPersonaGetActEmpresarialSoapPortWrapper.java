package com.bluedot.efactura.pool.wrappers;

import dgi.soap.rut.WSPersonaGetActEmpresarialSoapPort;

public class WSPersonaGetActEmpresarialSoapPortWrapper {

	private WSPersonaGetActEmpresarialSoapPort port;

	public WSPersonaGetActEmpresarialSoapPort getPort() {
		return port;
	}

	public void setPort(WSPersonaGetActEmpresarialSoapPort port) {
		this.port = port;
	}

	public WSPersonaGetActEmpresarialSoapPortWrapper(WSPersonaGetActEmpresarialSoapPort port) {
		super();
		this.port = port;
	}
	
}
