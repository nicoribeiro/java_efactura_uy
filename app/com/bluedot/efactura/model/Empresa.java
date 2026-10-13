package com.bluedot.efactura.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.Tuple;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
// Las empresas se buscan por RUT en todos lados (y de a miles en la sincronizacion de emisores electronicos)
@javax.persistence.Table(indexes = @javax.persistence.Index(name = "empresa_rut_idx", columnList = "rut"))
@ApiModel
public class Empresa extends Model<Empresa>{

	/**
	 * 
	 */
	private static final long serialVersionUID = -7989282112526964714L;

	@Id
	@GeneratedValue
	private int id;
	
	private String rut;
	
	private String razon;
	
	private String nombreComercial;
	
	private String mailRecepcion;
	
	private String hostRecepcion;
	
	private String userRecepcion;
	
	private String passRecepcion;
	
	private String puertoRecepcion;
	
	private boolean emisorElectronico;

	/*
	 * Datos de emisor electronico segun DGI (EFACCONSULTARRUCEMISORES y el archivo de emisores de DGI, ver
	 * EmisoresElectronicos). La razon social (DENOMINACION) va en razon y el mail de intercambio (MAIL) en mailRecepcion.
	 */

	/** Desde cuando es emisor electronico (FECHA_INICIO) */
	@javax.persistence.Temporal(javax.persistence.TemporalType.DATE)
	private java.util.Date fechaInicioEmisorDgi;

	/** Fin del periodo de transicion (FECHA_FIN_TRANSICION); solo lo trae el archivo */
	@javax.persistence.Temporal(javax.persistence.TemporalType.DATE)
	private java.util.Date fechaFinTransicionDgi;

	/**
	 * Fecha en la que deja de ser emisor electronico (FECHA_FIN). Se guarda porque DGI solo vuelve a informar un RUT si
	 * cambia: cuando la fecha pasa, la sincronizacion le saca emisorElectronico.
	 */
	@javax.persistence.Temporal(javax.persistence.TemporalType.DATE)
	private java.util.Date fechaFinEmisorDgi;

	/** URL del web service del proveedor de facturacion del emisor (URL_WEBSERVICE); solo lo trae el archivo */
	@javax.persistence.Column(columnDefinition = "TEXT")
	private String urlWebserviceDgi;

	/** Mail del contacto tecnico (MAIL_CONTACTO_TECNICO); solo lo trae el archivo */
	@javax.persistence.Column(columnDefinition = "TEXT")
	private String mailContactoTecnicoDgi;
	
	private String mailNotificaciones;
	
	private String fromEnvio; 
	
	private int offsetMail;
	
	/**
	 * Dias desde que se recibe un sobre hasta que se envia la respuesta a sus CFE (ME_) con la aceptacion o el rechazo
	 * de cada uno (ver RespuestaCfes). En ese plazo se puede rechazar un CFE desde el panel. 0 = enseguida, -1 = nunca
	 * automaticamente (solo a mano), null = el valor por defecto de la configuracion (RespuestaCFE.diasPorDefecto).
	 */
	private Integer diasRespuestaCfe;
	
	@ApiModelProperty(hidden = true)
	@Lob
	private byte[] logo;
	
	@ApiModelProperty(hidden = true)
	@OneToMany(cascade = CascadeType.ALL, mappedBy="empresa", fetch=FetchType.LAZY)
	private List<CAE> caes;
	
	@ApiModelProperty(hidden = true)
	@OneToMany(cascade = CascadeType.ALL, mappedBy="empresaEmisora", fetch=FetchType.LAZY)
	private List<SobreEmitido> sobresEmitidos;
	
	@ApiModelProperty(hidden = true)
	@OneToMany(cascade = CascadeType.ALL, mappedBy="empresaReceptora", fetch=FetchType.LAZY)
	private List<SobreRecibido> sobresRecibidos;
	
	private String paginaWeb;
	
	private String resolucion;
	
	@ApiModelProperty(hidden = true)
	@OneToOne(mappedBy="empresa")
	private FirmaDigital firmaDigital;
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy="empresa", fetch=FetchType.LAZY)
	private List<Sucursal> sucursales;
	
	@ApiModelProperty(hidden = true)
	@OneToMany(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private List<EmailMessage> emailsRecibidosError;

	public Empresa() {
		super();
	}

	public Empresa(String rut, String razon, String nombreComercial) {
		super();
		this.rut = rut;
		this.razon = razon;
		this.nombreComercial = nombreComercial;
	}
	
	private static Finder<Integer, Empresa> find = new Finder<Integer, Empresa>(Integer.class, Empresa.class);
	
	public static Empresa findById(Integer id) {
		return find.byId(id);
	}

	public static Empresa findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		Empresa empresa = find.byId(id);

		if (empresa == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.EMPRESA_NO_ENCONTRADA).withParams("id",id);
					

		return empresa;
	}
	
	public static Empresa findByRUT(String rut)
	{
		return find.query().eq("rut", rut).findUnique();
	}
	
	/** Empresas con esos RUT, por RUT (de a 1000 por consulta) */
	public static java.util.Map<String, Empresa> findByRUTs(java.util.Collection<String> ruts) {
		java.util.Map<String, Empresa> mapa = new java.util.HashMap<String, Empresa>();
		java.util.List<String> lista = new java.util.ArrayList<String>(ruts);
		for (int i = 0; i < lista.size(); i += 1000) {
			DefaultQuery<Empresa> q = (DefaultQuery<Empresa>) find.query();
			q.getCriteria().add(Restrictions.in("rut", lista.subList(i, Math.min(i + 1000, lista.size()))));
			// firmaDigital es el lado inverso del OneToOne: Hibernate la trae siempre, con un select por empresa si no va en
			// el mismo join (con miles de empresas eran miles de consultas)
			q.getCriteria().setFetchMode("firmaDigital", org.hibernate.FetchMode.JOIN);
			for (Empresa e : q.findList())
				mapa.put(e.getRut(), e);
		}
		return mapa;
	}

	public static List<Empresa> findEmpresasConMail()
	{
		return find.query().isNotNull("hostRecepcion").findList();
	}
	
	
	public static Empresa findByRUT(String rut, boolean throwExceptionWhenMissing) throws APIException {
		Empresa empresa = find.query().eq("rut", rut).findUnique();

		if (empresa == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.EMPRESA_NO_ENCONTRADA).withParams("rut", rut);

		return empresa;
	}
	
	public static List<Empresa> findAll()
	{
		return find.all();
	}
	
	/**
	 * Busqueda paginada de empresas.
	 *
	 * @param texto
	 *            busca en rut, razon y nombre comercial (contiene, sin distinguir mayusculas); null = sin filtro
	 * @param emisorElectronico
	 *            null = sin filtro
	 * @param soloConFirma
	 *            solo empresas con firma digital cargada (las que emiten con este sistema)
	 * @param ruts
	 *            solo estas empresas (control de acceso); null = sin restriccion, vacio = ninguna
	 * @return lista de la pagina y cantidad total de empresas que cumplen los filtros
	 */
	public static Tuple<List<Empresa>, Long> find(String texto, Boolean emisorElectronico, boolean soloConFirma,
			java.util.Collection<String> ruts, int page, int pageSize) {
		if (ruts != null && ruts.isEmpty())
			return new Tuple<List<Empresa>, Long>(new java.util.LinkedList<Empresa>(), 0l);

		DefaultQuery<Empresa> q = (DefaultQuery<Empresa>) find.query();

		if (ruts != null)
			q.getCriteria().add(Restrictions.in("rut", ruts));

		if (texto != null && !texto.trim().isEmpty()) {
			String like = "%" + texto.trim() + "%";
			q.getCriteria().add(Restrictions.or(Restrictions.ilike("rut", like), Restrictions.ilike("razon", like),
					Restrictions.ilike("nombreComercial", like)));
		}

		if (emisorElectronico != null)
			q.getCriteria().add(Restrictions.eq("emisorElectronico", emisorElectronico));

		if (soloConFirma)
			q.getCriteria().createAlias("firmaDigital", "firmaDigital", JoinType.INNER_JOIN);

		long rowCount = q.findRowCount();

		q.orderByAsc("rut");

		List<Empresa> list = q.findPage(page, pageSize);

		return new Tuple<List<Empresa>, Long>(list, rowCount);
	}

	/**
	 * Empresas que tienen firma digital o CAEs cargados, es decir las que emiten con este sistema. Evita recorrer
	 * todas las empresas (la tabla incluye la lista de emisores electronicos de DGI, decenas de miles).
	 */
	public static List<Empresa> findConFirmaDigitalOCAEs() {
		DetachedCriteria conFirmaDigital = DetachedCriteria.forClass(FirmaDigital.class, "firma")
				.setProjection(Projections.property("firma.empresa.id"));
		DetachedCriteria conCAEs = DetachedCriteria.forClass(CAE.class, "cae")
				.setProjection(Projections.property("cae.empresa.id"));

		DefaultQuery<Empresa> q = (DefaultQuery<Empresa>) find.query();
		q.getCriteria().add(Restrictions.or(Subqueries.propertyIn("id", conFirmaDigital), Subqueries.propertyIn("id", conCAEs)));

		return q.findList();
	}

	public static long count(){
		DefaultQuery<Empresa> q = (DefaultQuery<Empresa>) find.query();
		return q.findRowCount();
	}
	
	public static Empresa getOrCreateEmpresa(String docRecep, String rznSocRecep, String  nomComercial) {
		Empresa empresa = Empresa.findByRUT(docRecep);
		
		if (empresa == null) {
			/*
			 * Si la empresa no existe la registro como nueva en el sistema
			 */
			empresa = new Empresa(docRecep, rznSocRecep, nomComercial);
			empresa.save();
		}
		
		return empresa;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRut() {
		return rut;
	}

	public void setRut(String rut) {
		this.rut = rut;
	}

	public String getRazon() {
		return razon;
	}

	public void setRazon(String razon) {
		this.razon = razon;
	}

	public String getNombreComercial() {
		return nombreComercial;
	}

	public void setNombreComercial(String nombreComercial) {
		this.nombreComercial = nombreComercial;
	}

	public String getMailRecepcion() {
		return mailRecepcion;
	}

	public void setMailRecepcion(String mailRecepcion) {
		this.mailRecepcion = mailRecepcion;
	}

	public List<CAE> getCaes() {
		return caes;
	}

	public void setCaes(List<CAE> caes) {
		this.caes = caes;
	}

	public List<SobreEmitido> getSobresEmitidos() {
		return sobresEmitidos;
	}

	public void setSobresEmitidos(List<SobreEmitido> sobresEmitidos) {
		this.sobresEmitidos = sobresEmitidos;
	}

	public List<SobreRecibido> getSobresRecibidos() {
		return sobresRecibidos;
	}

	public void setSobresRecibidos(List<SobreRecibido> sobresRecibidos) {
		this.sobresRecibidos = sobresRecibidos;
	}

	public boolean isEmisorElectronico() {
		return emisorElectronico;
	}

	public void setEmisorElectronico(boolean emisorElectronico) {
		this.emisorElectronico = emisorElectronico;
	}

	public java.util.Date getFechaFinEmisorDgi() {
		return fechaFinEmisorDgi;
	}

	public void setFechaFinEmisorDgi(java.util.Date fechaFinEmisorDgi) {
		this.fechaFinEmisorDgi = fechaFinEmisorDgi;
	}

	public java.util.Date getFechaInicioEmisorDgi() {
		return fechaInicioEmisorDgi;
	}

	public void setFechaInicioEmisorDgi(java.util.Date fechaInicioEmisorDgi) {
		this.fechaInicioEmisorDgi = fechaInicioEmisorDgi;
	}

	public java.util.Date getFechaFinTransicionDgi() {
		return fechaFinTransicionDgi;
	}

	public void setFechaFinTransicionDgi(java.util.Date fechaFinTransicionDgi) {
		this.fechaFinTransicionDgi = fechaFinTransicionDgi;
	}

	public String getUrlWebserviceDgi() {
		return urlWebserviceDgi;
	}

	public void setUrlWebserviceDgi(String urlWebserviceDgi) {
		this.urlWebserviceDgi = urlWebserviceDgi;
	}

	public String getMailContactoTecnicoDgi() {
		return mailContactoTecnicoDgi;
	}

	public void setMailContactoTecnicoDgi(String mailContactoTecnicoDgi) {
		this.mailContactoTecnicoDgi = mailContactoTecnicoDgi;
	}

	/** Empresas marcadas como emisor electronico cuya fecha de fin en DGI ya llego */
	public static List<Empresa> findEmisoresVencidos(java.util.Date hoy) {
		DefaultQuery<Empresa> q = (DefaultQuery<Empresa>) find.query();
		q.getCriteria().add(Restrictions.eq("emisorElectronico", true));
		q.getCriteria().add(Restrictions.le("fechaFinEmisorDgi", hoy));
		q.getCriteria().setFetchMode("firmaDigital", org.hibernate.FetchMode.JOIN);
		return q.findList();
	}
	
	
	public BufferedImage logoAsImage() {
        InputStream in = new ByteArrayInputStream(logo);
        try {
			return ImageIO.read(in);
		} catch (IOException e) {
		}
        return null;
    }

    public void setLogo(BufferedImage image) {
        try {
        	ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG" /* for instance */, out);
			logo = out.toByteArray();
		} catch (IOException e) {
		}
    }

	public byte[] getLogo() {
		return logo;
	}

	public void setLogo(byte[] logo) {
		this.logo = logo;
	}

	public String getPaginaWeb() {
		return paginaWeb;
	}

	public void setPaginaWeb(String paginaWeb) {
		this.paginaWeb = paginaWeb;
	}

	public String getResolucion() {
		return resolucion;
	}

	public void setResolucion(String resolucion) {
		this.resolucion = resolucion;
	}

	public String getHostRecepcion() {
		return hostRecepcion;
	}

	public void setHostRecepcion(String hostRecepcion) {
		this.hostRecepcion = hostRecepcion;
	}

	public String getUserRecepcion() {
		return userRecepcion;
	}

	public void setUserRecepcion(String userRecepcion) {
		this.userRecepcion = userRecepcion;
	}

	public String getPassRecepcion() {
		return passRecepcion;
	}

	public void setPassRecepcion(String passRecepcion) {
		this.passRecepcion = passRecepcion;
	}

	public String getMailNotificaciones() {
		return mailNotificaciones;
	}

	public void setMailNotificaciones(String mailNotificaciones) {
		this.mailNotificaciones = mailNotificaciones;
	}

	public String getPuertoRecepcion() {
		return puertoRecepcion;
	}

	public void setPuertoRecepcion(String puertoRecepcion) {
		this.puertoRecepcion = puertoRecepcion;
	}

	public String getFromEnvio() {
		return fromEnvio;
	}

	public void setFromEnvio(String fromEnvio) {
		this.fromEnvio = fromEnvio;
	}

	public FirmaDigital getFirmaDigital() {
		return firmaDigital;
	}

	public void setFirmaDigital(FirmaDigital firmaDigital) {
		this.firmaDigital = firmaDigital;
	}

	public int getOffsetMail() {
		return offsetMail;
	}

	public Integer getDiasRespuestaCfe() {
		return diasRespuestaCfe;
	}

	public void setDiasRespuestaCfe(Integer diasRespuestaCfe) {
		this.diasRespuestaCfe = diasRespuestaCfe;
	}

	public void setOffsetMail(int offsetMail) {
		this.offsetMail = offsetMail;
	}

	public List<Sucursal> getSucursales() {
		return sucursales;
	}

	public void setSucursales(List<Sucursal> sucursales) {
		this.sucursales = sucursales;
	}

	public List<EmailMessage> getEmailsRecibidosError() {
		return emailsRecibidosError;
	}

	public void setEmailsRecibidosError(List<EmailMessage> emailsRecibidosError) {
		this.emailsRecibidosError = emailsRecibidosError;
	}
}
