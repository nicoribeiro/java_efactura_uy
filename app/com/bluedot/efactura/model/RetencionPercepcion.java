package com.bluedot.efactura.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.play4jpa.jpa.models.Model;

@Entity
public class RetencionPercepcion extends Model<RetencionPercepcion> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6938759842338709277L;
	
	@Id
	@GeneratedValue
	private long id;
	
	private String codigo;
	
	private double tasa;

	private double montoSujeto;
	
	private double valor;

	public RetencionPercepcion() {
		super();
	}
	
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public double getTasa() {
		return tasa;
	}

	public void setTasa(double tasa) {
		this.tasa = tasa;
	}

	public double getMontoSujeto() {
		return montoSujeto;
	}

	public void setMontoSujeto(double montoSujeto) {
		this.montoSujeto = montoSujeto;
	}

	public double getValor() {
		return valor;
	}

	public void setValor(double valor) {
		this.valor = valor;
	}
}
