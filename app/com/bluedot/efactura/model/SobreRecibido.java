package com.bluedot.efactura.model;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.security.EmailMessage;
import com.play4jpa.jpa.models.DefaultQuery;
@Entity
public class SobreRecibido extends Sobre{

	/**
	 * 
	 */
	private static final long serialVersionUID = -2509429294768242581L;
	
	/**
	 * Id que le otorga el emisor al sobre
	 */
	private Long idEmisor;
	
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

	public static List<SobreRecibido> findSobreRecibido(long idEmisor, Empresa empresaEmisora, Empresa empresaReceptora) {
		DefaultQuery<Sobre> q = (DefaultQuery<Sobre>) find.query();


		q.getCriteria().createAlias("empresaEmisora", "empresaEmisora", JoinType.LEFT_OUTER_JOIN);
		q.getCriteria().createAlias("empresaReceptora", "empresaReceptora", JoinType.LEFT_OUTER_JOIN);

		q.getCriteria().add(Restrictions.and

				(		Restrictions.eq("empresaEmisora.id", empresaEmisora.getId()),
						Restrictions.eq("empresaReceptora.id", empresaReceptora.getId())
						));

		
		List<Sobre> sobres = q.findList();
		
		LinkedList<SobreRecibido> sobresRecibidos = new LinkedList<SobreRecibido>();
		
		for (Iterator<Sobre> iterator = sobres.iterator(); iterator.hasNext();) {
			Sobre sobre = iterator.next();
			if (sobre instanceof SobreRecibido && ((SobreRecibido)sobre).getIdEmisor()==idEmisor)
				 sobresRecibidos.add((SobreRecibido) sobre);
		}
		
		return sobresRecibidos;
	}
	
	public long getIdEmisor() {
		return idEmisor;
	}


	public void setIdEmisor(long idEmisor) {
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

	public void setIdEmisor(Long idEmisor) {
		this.idEmisor = idEmisor;
	}
}
