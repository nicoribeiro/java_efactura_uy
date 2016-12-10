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

import play.db.jpa.JPAApi;
@Entity
public class UI extends Model<UI>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2902990737843920656L;

	@Id
	@GeneratedValue
	private int id;
	
	private int anio;
	
	private double cotizacion;

	public UI() {
		super();
	}

	public UI(int anio, double cotizacion) {
		super();
		this.anio = anio;
		this.cotizacion = cotizacion;
	}
	
	private static Finder<Integer, UI> find = new Finder<Integer, UI>(Integer.class, UI.class);
	
	public static UI findById(JPAApi jpaApi, Integer id) {
		return find.byId(jpaApi, id);
	}

	public static UI findById(JPAApi jpaApi, Integer id, boolean throwExceptionWhenMissing) throws APIException {
		UI ui = find.byId(jpaApi, id);

		if (ui == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.UI_NO_ENCONTRADA.withParams("id", id));
		return ui;
	}
	
	public static UI findByAnio(JPAApi jpaApi, int anio, boolean throwExceptionWhenMissing) throws APIException
	{
		DefaultQuery<UI> q = (DefaultQuery<UI>) find.query(jpaApi);
		
			
			q.getCriteria().add(Restrictions.and
					
					(		Restrictions.eq("anio", anio)
					));
		
		UI ui = q.findUnique();
		if (ui == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.UI_NO_ENCONTRADA.withParams("anio", anio));
		return ui;
	}

	public int getAnio() {
		return anio;
	}

	public void setAnio(int anio) {
		this.anio = anio;
	}

	public double getCotizacion() {
		return cotizacion;
	}

	public void setCotizacion(double cotizacion) {
		this.cotizacion = cotizacion;
	}

	
}
