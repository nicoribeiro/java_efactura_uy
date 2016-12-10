package com.bluedot.efactura.model;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.criterion.Restrictions;

import com.bluedot.commons.error.APIException;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;

@Entity
public class Titular extends Model<Titular> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -219869157871696215L;

	@Id
	@GeneratedValue
	private int id;

	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Pais paisEmisorDocumento;

	@Enumerated(EnumType.STRING)
	private TipoDocumento tipoDocumento;

	private String documento;

	/**
	 * Tipo de facturacion, por defecto solo impreso
	 */
	private TipoFacturacionElectronica tipoFacturacionElectronica;

	/**
	 * Email para envío de Comprobantes Electrónicos
	 */
	private String email;

	public Titular() {
		super();
	}

	private static Finder<Integer, Titular> find = new Finder<Integer, Titular>(Integer.class, Titular.class);

	public static Titular findById(JPAApi jpaApi, Integer id) {
		return find.byId(jpaApi, id);
	}

	public static Titular findById(JPAApi jpaApi, Pais paisEmisorDocumento, TipoDocumento tipoDocumento, String documento)
			throws APIException {
		DefaultQuery<Titular> q = (DefaultQuery<Titular>) find.query(jpaApi);

		q.getCriteria().add(Restrictions.and

		(Restrictions.eq("paisEmisorDocumento", paisEmisorDocumento), Restrictions.eq("tipoDocumento", tipoDocumento),
				Restrictions.eq("documento", documento)));

		Titular titular = q.findUnique();
		return titular;
	}

	public Titular(Pais paisEmisorDocumento, TipoDocumento tipoDocumento,
			TipoFacturacionElectronica tipoFacturacionElectronica, String documento) {
		super();
		this.paisEmisorDocumento = paisEmisorDocumento;
		this.tipoDocumento = tipoDocumento;
		this.tipoFacturacionElectronica = tipoFacturacionElectronica;
		this.documento = documento;
	}

	public Titular(Pais paisEmisorDocumento, TipoDocumento tipoDocumento, String documento) {
		super();
		this.paisEmisorDocumento = paisEmisorDocumento;
		this.tipoDocumento = tipoDocumento;
		this.tipoFacturacionElectronica = TipoFacturacionElectronica.SOLO_IMPRESA;
		this.documento = documento;
	}

	public Pais getPaisEmisorDocumento() {
		return paisEmisorDocumento;
	}

	public void setPaisEmisorDocumento(Pais paisEmisorDocumento) {
		this.paisEmisorDocumento = paisEmisorDocumento;
	}

	public TipoDocumento getTipoDocumento() {
		return tipoDocumento;
	}

	public void setTipoDocumento(TipoDocumento tipoDocumento) {
		this.tipoDocumento = tipoDocumento;
	}

	public TipoFacturacionElectronica getTipoFacturacionElectronica() {
		return tipoFacturacionElectronica;
	}

	public void setTipoFacturacionElectronica(TipoFacturacionElectronica tipoFacturacionElectronica) {
		this.tipoFacturacionElectronica = tipoFacturacionElectronica;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDocumento() {
		return documento;
	}

	public void setDocumento(String documento) {
		this.documento = documento;
	}

}
