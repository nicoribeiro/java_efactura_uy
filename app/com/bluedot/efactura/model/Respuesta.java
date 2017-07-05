package com.bluedot.efactura.model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import org.hibernate.annotations.Type;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

@Entity
public class Respuesta extends Model<Respuesta> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5867483952743687539L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.SEQUENCE,generator="respuestas_seq")
	@SequenceGenerator(name = "respuestas_seq", sequenceName = "respuestas_seq" )
	private int id;

	private String nombreArchivo;
	
	@Type(type="text")
	private String payload;
	
	public Respuesta() {
		super();
	}

	private static Finder<Integer, Respuesta> find = new Finder<Integer, Respuesta>(Integer.class, Respuesta.class);

	public static Respuesta findById(Integer id) {
		return find.byId(id);
	}

	public static Respuesta findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		Respuesta pais = find.byId(id);

		if (pais == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.RESPUESTA_NO_ENCONTRADA.withParams("id",id));

		return pais;
	}
	
	public static long count(){
		DefaultQuery<Respuesta> q = (DefaultQuery<Respuesta>) find.query();
		return q.findRowCount();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String getNombreArchivo() {
		return nombreArchivo;
	}

	public void setNombreArchivo(String nombreArchivo) {
		this.nombreArchivo = nombreArchivo;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

}
