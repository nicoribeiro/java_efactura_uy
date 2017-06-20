package com.bluedot.efactura.model;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.XML;
import com.play4jpa.jpa.models.DefaultQuery;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.EnvioCFE;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
@Entity
public class SobreEmitido extends Sobre{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 405831344290550640L;
	
	/**
	 * AS - Sobre Recibido 
	 * 
	 * BS - Sobre Rechazado
	 * 
	 * Es el estado de la respuesta al sobre enviado a DGI
	 */
	@Enumerated(EnumType.STRING)
	private EstadoACKSobreType estadoDgi;
	
	/**
	 * Es el xml que se envio DGI
	 */
	@Type(type="text")
	private String xmlDgi;
	
	/**
	 * Datetime estimado para la consulta del resultado de los CFE incluidos.
	 * 
	 * Este campo es relevante si destino==DestinoSobre.CLIENTE
	 */
	private Date fechaConsulta;
	
	/**
	 * Respuesta a la recepcion del sobre.
	 */
	@Type(type="text")
	private String respuesta_dgi;
	
	/**
	 * Resultado del procesamiento de los CFEs del sobre. Emitido por DGI
	 */
	@Type(type="text")
	private String resultado_dgi;
	
	private Long idReceptor;
	
	private String token;
	
	@Transient
	private EnvioCFE envioCFE;
	
	@Transient
	private boolean reenvio = false;
	
	public SobreEmitido() {
		super();
	}

	public SobreEmitido(Empresa empresaEmisora, Empresa empresaReceptora, String nombreArchivo, int cantComprobantes,
			List<CFE> cfes) {
		super(empresaEmisora, empresaReceptora, nombreArchivo, cantComprobantes, cfes);
	}
	
	public static List<SobreEmitido> findByEmpresaEmisoraAndDate(Empresa empresaEmisora, Date fecha) throws APIException
	{
		DefaultQuery<Sobre> q = (DefaultQuery<Sobre>) find.query();
		
			
			q.getCriteria().createAlias("empresaEmisora", "empresa", JoinType.LEFT_OUTER_JOIN);
			
			q.getCriteria().add(Restrictions.and
					
					(		Restrictions.eq("empresa.id", empresaEmisora.getId()), 
							Restrictions.eq("fecha", fecha)
					));
		
		LinkedList<SobreEmitido> sobresEmitidos = new LinkedList<SobreEmitido>();
			
			
		List<Sobre> sobres = q.findList();
		
		for (Iterator<Sobre> iterator = sobres.iterator(); iterator.hasNext();) {
			Sobre sobre = iterator.next();
			if (sobre instanceof SobreEmitido)
				sobresEmitidos.add((SobreEmitido) sobre);
		}
		
		return sobresEmitidos;
		
	}
	
	public Date getFechaConsulta() {
		return fechaConsulta;
	}

	public void setFechaConsulta(Date fechaConsulta) {
		this.fechaConsulta = fechaConsulta;
	}
	
	public String getXmlDgi() {
		return xmlDgi;
	}

	public void setXmlDgi(String xml) {
		this.xmlDgi = xml;
	}

	public String getRespuesta_dgi() {
		return respuesta_dgi;
	}

	public void setRespuesta_dgi(String respuesta_dgi) {
		this.respuesta_dgi = respuesta_dgi;
	}

	public String getResultado_dgi() {
		return resultado_dgi;
	}

	public void setResultado_dgi(String resultado_dgi) {
		this.resultado_dgi = resultado_dgi;
	}

	public Long getIdReceptor() {
		return idReceptor;
	}

	public void setIdReceptor(Long idReceptor) {
		this.idReceptor = idReceptor;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public EnvioCFE getEnvioCFE() {
		if (envioCFE==null)
			try {
				envioCFE = (EnvioCFE) XML.unMarshall(XML.loadXMLFromString(xmlDgi), EnvioCFE.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return envioCFE;
	}

	public void setEnvioCFE(EnvioCFE envioCFE) {
		this.envioCFE = envioCFE;
	}

	public boolean isReenvio() {
		return reenvio;
	}

	public void setReenvio(boolean reenvio) {
		this.reenvio = reenvio;
	}

	public EstadoACKSobreType getEstadoDgi() {
		return estadoDgi;
	}

	public void setEstadoDgi(EstadoACKSobreType estadoDGI) {
		this.estadoDgi = estadoDGI;
	}	
}
