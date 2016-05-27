package com.bluedot.efactura.pool;

import dgi.soap.recepcion.WSEFacturaSoapPort;

public class WSEFacturaSoapPortWrapper {

	private WSEFacturaSoapPort port;

	public WSEFacturaSoapPort getPort() {
		return port;
	}

	public void setPort(WSEFacturaSoapPort port) {
		this.port = port;
	}

	public WSEFacturaSoapPortWrapper(WSEFacturaSoapPort port) {
		super();
		this.port = port;
	}
	
}
