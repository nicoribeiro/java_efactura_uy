package com.bluedot.efactura.serializers;

import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.serializers.JSONSerializer;
import com.bluedot.commons.serializers.JSONSerializerProvider;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.Respuesta;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.Sucursal;
import com.bluedot.efactura.model.Titular;

import dgi.classes.entreEmpresas.CFEEmpresasType;

public class EfacturaJSONSerializerProvider extends JSONSerializerProvider {

	public static JSONSerializer<Pais> getPaisSerializer() {
		return new PaisSerializer<Pais>();
	}

	public static JSONSerializer<Empresa> getEmpresaSerializer() {
		return new EmpresaSerializer<Empresa>(getSucursalSerializer());
	}
	
	public static JSONSerializer<CFE> getCFESerializer(){
		CFESerializer<CFE> serializer = new CFESerializer<CFE>(getEmpresaSerializer(), getDetalleSerializer(), getCAESerializer(), getTitularSerializer());
		serializer.setSobreSerializer(getSobreSerializer(serializer));
		return serializer;
	}
	
	public static JSONSerializer<Detalle> getDetalleSerializer() {
		return new DetalleSerializer<Empresa>();
	}

	public static JSONSerializer<ReporteDiario> getReporteDiarioSerializer() {
		return new ReporteDiarioSerializer<ReporteDiario>(getEmpresaSerializer());
	}
	
	public static JSONSerializer<CFEEmpresasTypeWrapper> getCFEEmpresasTypeSerializer() {
		return new CFEEmpresasTypeSerializer<CFEEmpresasTypeWrapper>();
	}
	
	public static JSONSerializer<CAE> getCAESerializer(){
		return new CAESerializer<CAE>(getEmpresaSerializer());
	}
	
	public static JSONSerializer<Titular> getTitularSerializer(){
		return new TitularSerializer<Titular>(getPaisSerializer());
	}
	
	public static JSONSerializer<Sobre> getSobreSerializer(){
		return new SobreSerializer<CAE>(getEmpresaSerializer(), getRespuestaSerializer(), getCFESerializer());
	}

	public static JSONSerializer<Sobre> getSobreSerializer(JSONSerializer<CFE> cfeSerializer){
		return new SobreSerializer<CAE>(getEmpresaSerializer(), getRespuestaSerializer(), cfeSerializer);
	}
	
	private static JSONSerializer<Respuesta> getRespuestaSerializer() {
		return new RespuestaSerializer<Respuesta>();
	}
	
	public static JSONSerializer<Sucursal> getSucursalSerializer() {
		return new SucursalSerializer<Sucursal>();
	}

}
