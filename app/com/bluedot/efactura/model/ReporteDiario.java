package com.bluedot.efactura.model;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import dgi.classes.reporte.ReporteDefType;
import dgi.classes.respuestas.reporte.EstadoACKRepType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel
public class ReporteDiario extends Model<ReporteDiario>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3829743720106426694L;

	@Id
	@GeneratedValue
	private int id;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Empresa empresa;
	
	@Temporal(TemporalType.DATE)
	private Date fecha;
	
	private int secuencial;
	
	private Date timestampEnviado;
	
	@Type(type="text")
	private String xml;
	
	/**
	 * Contine el xml de respuesta de la DGI
	 */
	@Type(type="text")
	private String respuesta;
	
	@ApiModelProperty(hidden = true)
	@OneToMany(cascade = CascadeType.ALL, mappedBy="reporteDiario", fetch=FetchType.LAZY)
	private List<CFE> cfes;
	
	/**
	 * AR - Reporte Recibido 
	 * 
	 * BR - Reporte Rechazado
	 */
	@Enumerated(EnumType.STRING)
	private EstadoACKRepType estado;
	
	@ApiModelProperty(hidden = true)
	@Enumerated(EnumType.STRING)
	private MotivoRechazoReporte motivo;
	
	@ApiModelProperty(hidden = true)
	@Transient
	private ReporteDefType reporteDefType;
	
	private String idReceptor;

	public ReporteDiario() {
		super();
	}

	public ReporteDiario(Empresa empresa, Date fecha) {
		super();
		this.empresa = empresa;
		this.fecha = fecha;
		
		int secuencial = 0;
		
		List<ReporteDiario> reportes = findByEmpresaFecha(empresa, fecha);
		
		for (Iterator<ReporteDiario> iterator = reportes.iterator(); iterator.hasNext();) {
			ReporteDiario reporteDiario = iterator.next();
			if (reporteDiario.getSecuencial() > secuencial && reporteDiario.getEstado()!=null && reporteDiario.getEstado()==EstadoACKRepType.AR)
				secuencial = reporteDiario.getSecuencial();
		}
		
		this.secuencial = secuencial+1;
	}
	
	private static Finder<Integer, ReporteDiario> find = new Finder<Integer, ReporteDiario>(Integer.class, ReporteDiario.class);
	
	public static ReporteDiario findById(Integer id) {
		return find.byId(id);
	}

	public static ReporteDiario findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		ReporteDiario reporteDiario = find.byId(id);

		if (reporteDiario == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.REPORTE_DIARIO_NO_ENCONTRADO).withParams("id",id);
		return reporteDiario;
	}

	
	public static List<ReporteDiario> findByEmpresaFecha(Empresa empresa, Date fecha)
	{
		DefaultQuery<ReporteDiario> q = (DefaultQuery<ReporteDiario>) find.query();
		
			
			q.getCriteria().createAlias("empresa", "empresa", JoinType.LEFT_OUTER_JOIN);
			
			q.getCriteria().add(Restrictions.and
					
					(		Restrictions.eq("empresa.id", empresa.getId()), 
							Restrictions.eq("fecha",fecha) 
					));
		
		return q.findList();
	}


	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public int getSecuencial() {
		return secuencial;
	}

	public void setSecuencial(int secuencial) {
		this.secuencial = secuencial;
	}

	public Date getTimestampEnviado() {
		return timestampEnviado;
	}

	public void setTimestampEnviado(Date timestampEnviado) {
		this.timestampEnviado = timestampEnviado;
	}

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public String getRespuesta() {
		return respuesta;
	}

	public void setRespuesta(String respuesta) {
		this.respuesta = respuesta;
	}

	public List<CFE> getCfes() {
		return cfes;
	}

	public void setCfes(List<CFE> cfes) {
		this.cfes = cfes;
	}

	public EstadoACKRepType getEstado() {
		return estado;
	}

	public void setEstado(EstadoACKRepType estado) {
		this.estado = estado;
	}

	public MotivoRechazoReporte getMotivo() {
		return motivo;
	}

	public void setMotivo(MotivoRechazoReporte motivo) {
		this.motivo = motivo;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public ReporteDefType getReporteDefType() {
		return reporteDefType;
	}

	public void setReporteDefType(ReporteDefType reporteDefType) {
		this.reporteDefType = reporteDefType;
	}

	public String getIdReceptor() {
		return idReceptor;
	}

	public void setIdReceptor(String idReceptor) {
		this.idReceptor = idReceptor;
	}

}
