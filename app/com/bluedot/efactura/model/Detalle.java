package com.bluedot.efactura.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.play4jpa.jpa.models.Model;

@Entity
public class Detalle extends Model<Detalle> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3701910824297362919L;

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private CFE cfe;

	/**
	 * nro de linea dentro del CFE
	 */
	private int nroLinea;
	
	private String tpoCod;
	
	private String codItem;

	private String nombreItem;
	
	private String descripcionItem;

	private double cantidad;

	private String unidadMedida;

	private double precioUnitario;

	private double descuentoPorcentaje;

	private double descuentoMonto;

	private double recargoPorcentaje;

	private double recargoMonto;

	private double tasaRecepPercep;

	private double montoSujetoRecepPercep;

	/**
	 * montoItem=Precio*cantidad - descuento + recargo
	 */
	private double montoItem;

	public Detalle() {
		super();
	}

	public Detalle(CFE cfe, int nroLinea, String nombreItem, double cantidad, String unidadMedida, double precioUnitario,
			double montoItem) {
		super();
		this.cfe = cfe;
		this.nroLinea = nroLinea;
		this.nombreItem = nombreItem;
		this.cantidad = cantidad;
		this.unidadMedida = unidadMedida;
		this.precioUnitario = precioUnitario;
		this.montoItem = montoItem;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public CFE getCfe() {
		return cfe;
	}

	public void setCfe(CFE cfe) {
		this.cfe = cfe;
	}

	public int getNroLinea() {
		return nroLinea;
	}

	public void setNroLinea(int nroLinea) {
		this.nroLinea = nroLinea;
	}

	public String getNombreItem() {
		return nombreItem;
	}

	public void setNombreItem(String nombreItem) {
		this.nombreItem = nombreItem;
	}

	public double getCantidad() {
		return cantidad;
	}

	public void setCantidad(double cantidad) {
		this.cantidad = cantidad;
	}

	public String getUnidadMedida() {
		return unidadMedida;
	}

	public void setUnidadMedida(String unidadMedida) {
		this.unidadMedida = unidadMedida;
	}

	public double getPrecioUnitario() {
		return precioUnitario;
	}

	public void setPrecioUnitario(double precioUnitario) {
		this.precioUnitario = precioUnitario;
	}

	public double getDescuentoPorcentaje() {
		return descuentoPorcentaje;
	}

	public void setDescuentoPorcentaje(double descuentoPorcentaje) {
		this.descuentoPorcentaje = descuentoPorcentaje;
	}

	public double getDescuentoMonto() {
		return descuentoMonto;
	}

	public void setDescuentoMonto(double descuentoMonto) {
		this.descuentoMonto = descuentoMonto;
	}

	public double getRecargoPorcentaje() {
		return recargoPorcentaje;
	}

	public void setRecargoPorcentaje(double recargoPorcentaje) {
		this.recargoPorcentaje = recargoPorcentaje;
	}

	public double getRecargoMonto() {
		return recargoMonto;
	}

	public void setRecargoMonto(double recargoMonto) {
		this.recargoMonto = recargoMonto;
	}

	public double getTasaRecepPercep() {
		return tasaRecepPercep;
	}

	public void setTasaRecepPercep(double tasaRecepPercep) {
		this.tasaRecepPercep = tasaRecepPercep;
	}

	public double getMontoSujetoRecepPercep() {
		return montoSujetoRecepPercep;
	}

	public void setMontoSujetoRecepPercep(double montoSujetoRecepPercep) {
		this.montoSujetoRecepPercep = montoSujetoRecepPercep;
	}

	public double getMontoItem() {
		return montoItem;
	}

	public void setMontoItem(double montoItem) {
		this.montoItem = montoItem;
	}

	public String getCodItem() {
		return codItem;
	}

	public void setCodItem(String codItem) {
		this.codItem = codItem;
	}

	public String getDescripcionItem() {
		return descripcionItem;
	}

	public void setDescripcionItem(String descripcionItem) {
		this.descripcionItem = descripcionItem;
	}

	public String getTpoCod() {
		return tpoCod;
	}

	public void setTpoCod(String tpoCod) {
		this.tpoCod = tpoCod;
	}

}
