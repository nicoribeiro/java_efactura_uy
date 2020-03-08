package com.bluedot.efactura.model;

import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.play4jpa.jpa.models.Model;

@Entity
public class CAE extends Model<CAE>{
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = -1416557370096579792L;

	@Id
	@GeneratedValue
	private int id;
	
	/**
	 * Empresa poseedora del CAE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Empresa empresa;
	
	/**
	 * Número asignado por DGI a la Constancia de autorización de emisión
	 */
	private long nro;
	
	/**
	 * Tipo de comprobante
	 */
	@Enumerated(EnumType.STRING)
	private TipoDoc tipo;
	
	private String serie;
	
	/**
	 * Nro Inicial del CAE
	 */
	private long inicial;
	
	/**
	 * Nro Final del CAE
	 */
	private long fin;
	
	/**
	 * Fecha de vencimiento del CAE
	 */
	private Date fechaVencimiento;
	
	/**
	 * Sigiente Nro a utilizar, siempre inicial<=siguiente<=fin
	 */
	private long siguiente;
	
	/**
	 * Fecha de Anulado o vencido. Esta fecha de registra cuando se anula manualmente o el día que vence. En el reporte diario de ese día debe ir a DGI todo el rango no utilizado.
	 */
	private Date fechaAnulado;
	
	public CAE() {
		super();
	}

	public CAE(Empresa empresa, long nro, TipoDoc tipo, String serie, long inicial, long fin, Date fechaVencimiento, long siguiente) {
		super();
		this.empresa = empresa;
		this.nro = nro;
		this.tipo = tipo;
		this.serie = serie;
		this.inicial = inicial;
		this.fin = fin;
		this.fechaVencimiento = fechaVencimiento;
		this.siguiente = siguiente;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public long getNro() {
		return nro;
	}

	public void setNro(long nro) {
		this.nro = nro;
	}

	public TipoDoc getTipo() {
		return tipo;
	}

	public void setTipo(TipoDoc tipo) {
		this.tipo = tipo;
	}

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public long getInicial() {
		return inicial;
	}

	public void setInicial(long inicial) {
		this.inicial = inicial;
	}

	public long getFin() {
		return fin;
	}

	public void setFin(long fin) {
		this.fin = fin;
	}

	public Date getFechaVencimiento() {
		return fechaVencimiento;
	}

	public void setFechaVencimiento(Date fechaVencimiento) {
		this.fechaVencimiento = fechaVencimiento;
	}

	public long getSiguiente() {
		return siguiente;
	}

	public void setSiguiente(long siguiente) {
		this.siguiente = siguiente;
	}

	public Date getFechaAnulado() {
		return fechaAnulado;
	}

	public void setFechaAnulado(Date fechaAnulado) {
		this.fechaAnulado = fechaAnulado;
	}

}
