package com.bluedot.efactura.microControllers.implementation;

import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Sucursal;

public class MicroControllerDefault {

	private Empresa empresa;
	
	private Sucursal sucursal;
	
	public MicroControllerDefault(Empresa empresa) {
		this.empresa = empresa;
	}
	
	public MicroControllerDefault(Sucursal sucursal) {
		this.empresa = sucursal.getEmpresa();
		this.sucursal = sucursal;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public Sucursal getSucursal() {
		return sucursal;
	}

	public void setSucursal(Sucursal sucursal) {
		this.sucursal = sucursal;
	}

}
