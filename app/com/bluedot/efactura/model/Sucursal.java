package com.bluedot.efactura.model;

import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import io.swagger.annotations.ApiModel;
import views.html.helper.form;

@Entity
@ApiModel
public class Sucursal extends Model<Sucursal>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
	private int id;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Empresa empresa;
	
	private String domicilioFiscal;
	
	private String ciudad;
	
	private String departamento;
	
	private String telefono;
	
	private Integer codigoSucursal;
	
	private String codigoPostal;
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy="sucursal", fetch=FetchType.LAZY)
	private List<CFE> cfes;
	
	
	public Sucursal() {
		super();
	}
	
	private static Finder<Integer, Sucursal> find = new Finder<Integer, Sucursal>(Integer.class, Sucursal.class);
	
	public static Sucursal findById(Integer id) {
		return find.byId(id);
	}

	public static Sucursal findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		Sucursal empresa = find.byId(id);

		if (empresa == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.SUCURSAL_NO_ENCONTRADA).withParams("id",id);
					

		return empresa;
	}
	
	public static Sucursal findByCodigoSucursal(List<Sucursal> sucursales, int codigoSucursal){
		for(Sucursal sucursal:sucursales) {
			if (sucursal.getCodigoSucursal()!=null && sucursal.getCodigoSucursal()==codigoSucursal)
				return sucursal;
		}
		return null;
	}
	
	public static Sucursal findByCodigoSucursal(List<Sucursal> sucursales, int codigoSucursal, boolean throwExceptionWhenMissing) throws APIException {
		Sucursal sucursal = findByCodigoSucursal(sucursales, codigoSucursal);

		if (sucursal == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.SUCURSAL_NO_ENCONTRADA).withParams("codigoSucursal", codigoSucursal);

		return sucursal;
	}
		
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDepartamento() {
		return departamento;
	}

	public void setDepartamento(String departamento) {
		this.departamento = departamento;
	}

	public Integer getCodigoSucursal() {
		return codigoSucursal;
	}	
		
	public String getCodigoPostal() {
		return codigoPostal;
	}

	public void setCodigoPostal(String codigoPostal) {
		this.codigoPostal = codigoPostal;
	}

	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getTelefono() {
		return telefono;
	}

	public void setTelefono(String telefono) {
		this.telefono = telefono;
	}

	public void setCodigoSucursal(Integer codigoSucursal) {
		this.codigoSucursal = codigoSucursal;
	}

	public String getCiudad() {
		return ciudad;
	}

	public void setCiudad(String ciudad) {
		this.ciudad = ciudad;
	}

	public String getDomicilioFiscal() {
		return domicilioFiscal;
	}

	public void setDomicilioFiscal(String domicilioFiscal) {
		this.domicilioFiscal = domicilioFiscal;
	}

}
