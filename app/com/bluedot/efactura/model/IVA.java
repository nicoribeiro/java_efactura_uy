package com.bluedot.efactura.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;

@Entity
public class IVA extends Model<IVA>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 4473937981306085132L;

	@Id
	@GeneratedValue
	private int id;
	
	private int indiceFacturacion;
	
	private double porcentajeIVA;

	public IVA() {
		super();
	}
	
	public IVA(int indiceFacturacion, double porcentajeIVA) {
		super();
		this.indiceFacturacion = indiceFacturacion;
		this.porcentajeIVA = porcentajeIVA;
	}
	
	public IVA(IndicadorFacturacion indicadorFacturacion, double porcentajeIVA) {
		super();
		this.indiceFacturacion = indicadorFacturacion.getIndice();
		this.porcentajeIVA = porcentajeIVA;
	}

	private static Finder<Integer, IVA> find = new Finder<Integer, IVA>(Integer.class, IVA.class);
	
	public static IVA findById(JPAApi jpaApi, Integer id) {
		return find.byId(jpaApi, id);
	}
	
	public static List<IVA> findAll(JPAApi jpaApi) {
		return find.all(jpaApi);
	}
	
	public static IVA findByIndicadorFacturacion(JPAApi jpaApi, IndicadorFacturacion indicadorFacturacion)
	{
		return find.query(jpaApi).eq("indiceFacturacion", indicadorFacturacion.getIndice()).findUnique();
	}
	
	public static long count(JPAApi jpaApi){
		DefaultQuery<IVA> q = (DefaultQuery<IVA>) find.query(jpaApi);
		return q.findRowCount();
	}

	public int getIndiceFacturacion() {
		return indiceFacturacion;
	}

	public void setIndiceFacturacion(int indiceFacturacion) {
		this.indiceFacturacion = indiceFacturacion;
	}

	public double getPorcentajeIVA() {
		return porcentajeIVA;
	}

	public void setPorcentajeIVA(double porcentajeIVA) {
		this.porcentajeIVA = porcentajeIVA;
	}

}
