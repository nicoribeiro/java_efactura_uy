package com.bluedot.efactura.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
@Entity
public class SobreRecibido extends Sobre{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2509429294768242581L;
	
	/**
	 * Id que le otorga el emisor al sobre
	 */
	private String idEmisor;
	
	/**
	 * Fecha recibido por el sistema de correo
	 */
	private Date timestampRecibido;
	
	/**
	 * Fecha en que se procesa el sobre
	 */
	private Date timestampProcesado;
	
	
	public SobreRecibido() {
		super();
	}


	public SobreRecibido(Empresa empresaEmisora, Empresa empresaReceptora, String nombreArchivo, int cantComprobantes,
			List<CFE> cfes) {
		super(empresaEmisora, empresaReceptora, nombreArchivo, cantComprobantes, cfes);
	}


	public String getIdEmisor() {
		return idEmisor;
	}


	public void setIdEmisor(String idEmisor) {
		this.idEmisor = idEmisor;
	}


	public Date getTimestampRecibido() {
		return timestampRecibido;
	}


	public void setTimestampRecibido(Date timestampRecibido) {
		this.timestampRecibido = timestampRecibido;
	}


	public Date getTimestampProcesado() {
		return timestampProcesado;
	}


	public void setTimestampProcesado(Date timestampProcesado) {
		this.timestampProcesado = timestampProcesado;
	}
}
