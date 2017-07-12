package com.bluedot.efactura.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.hibernate.criterion.Restrictions;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

@Entity
public class Pais extends Model<Pais> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6137011152329960664L;

	@Id
	@GeneratedValue
	private int id;

	/*
	 * Código ISO 3166-1 alfa-2
	 */
	private String codigo;

	private String descripcion;

	public Pais() {
		super();
	}

	public Pais(String codigo, String descripcion) {
		super();
		this.codigo = codigo;
		this.descripcion = descripcion;
	}

	private static Finder<Integer, Pais> find = new Finder<Integer, Pais>(Integer.class, Pais.class);

	public static Pais findById(Integer id) {
		return find.byId(id);
	}

	public static Pais findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		Pais pais = find.byId(id);

		if (pais == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.PAIS_NO_ENCONTRADO).withParams("id",id);

		return pais;
	}
	
	public static Pais findByCodigo(String codigo, boolean throwExceptionWhenMissing) throws APIException {

		DefaultQuery<Pais> q = (DefaultQuery<Pais>) find.query();

		q.getCriteria().add(Restrictions.eq("codigo", codigo));

		Pais pais = q.findUnique();
		
		if (pais == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.PAIS_NO_ENCONTRADO).withParams("codigo",codigo);

		return pais;
	}
	
	
	public static long count(){
		DefaultQuery<Pais> q = (DefaultQuery<Pais>) find.query();
		return q.findRowCount();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCodigo() {
		return codigo;
	}

	public void setCodigo(String codigo) {
		this.codigo = codigo;
	}

	public String getDescripcion() {
		return descripcion;
	}

	public void setDescripcion(String descripcion) {
		this.descripcion = descripcion;
	}

}
