package com.bluedot.efactura.serializers;

import com.bluedot.efactura.model.Empresa;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public class CFEEmpresasTypeWrapper {

	private CFEEmpresasType cfeEmpresasType;
	private Empresa empresaReceptora;
	
	public CFEEmpresasTypeWrapper(CFEEmpresasType cfeEmpresasType, Empresa empresaReceptora) {
		super();
		this.cfeEmpresasType = cfeEmpresasType;
		this.empresaReceptora = empresaReceptora;
	}
	
	public CFEEmpresasType getCfeEmpresasType() {
		return cfeEmpresasType;
	}
	
	public void setCfeEmpresasType(CFEEmpresasType cfeEmpresasType) {
		this.cfeEmpresasType = cfeEmpresasType;
	}
	
	public Empresa getEmpresaReceptora() {
		return empresaReceptora;
	}
	
	public void setEmpresaReceptora(Empresa empresaReceptora) {
		this.empresaReceptora = empresaReceptora;
	}
	
}
