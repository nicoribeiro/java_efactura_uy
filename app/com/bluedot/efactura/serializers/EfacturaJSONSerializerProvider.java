package com.bluedot.efactura.serializers;

import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.commons.serializers.JSONSerializerProvider;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.ReporteDiario;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public class EfacturaJSONSerializerProvider extends JSONSerializerProvider {

	public static JSONSerializer<Pais> getPaisSerializer() {
		return new PaisSerializer<Pais>();
	}

	public static JSONSerializer<Empresa> getEmpresaSerializer() {
		return new EmpresaSerializer<Empresa>();
	}
	
	public static JSONSerializer<CFE> getCFESerializer(){
		return new CFESerializer<CFE>(getEmpresaSerializer(), getDetalleSerializer());
	}
	
	public static JSONSerializer<Detalle> getDetalleSerializer() {
		return new DetalleSerializer<Empresa>();
	}

	public static JSONSerializer<ReporteDiario> getReporteDiarioSerializer() {
		return new ReporteDiarioSerializer<ReporteDiario>(getEmpresaSerializer());
	}
	
	public static JSONSerializer<CFEEmpresasType> getCFEEmpresasTypeSerializer() {
		return new CFEEmpresasTypeSerializer<CFEEmpresasType>();
	}

}
