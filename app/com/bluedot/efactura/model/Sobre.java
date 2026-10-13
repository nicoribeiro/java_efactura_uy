package com.bluedot.efactura.model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.XML;
import com.bluedot.commons.utils.Tuple;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;


@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
@ApiModel(value = "Sobre", subTypes = { SobreEmitido.class, SobreRecibido.class })
public abstract class Sobre extends Model<Sobre> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8050514407296677614L;

	static final String FORMATO_NOMBRE_SOBRE = "Sob_<RUC>_<FECHA>_<ID_EMISOR>.xml";
	static final String FORMATO_ACK_SOBRE = "M_<ID_RECEPTOR>_Sob_<RUC>_<FECHA>_<ID_EMISOR>.xml";
	static final String FORMATO_ACK_CFE = "ME_<ID_RECEPTOR>_Sob_<RUC>_<FECHA>_<ID_EMISOR>.xml";
	
	static final SimpleDateFormat formatoFechaSobre = new SimpleDateFormat("yyyyMMdd");
	
	@Id
	@GeneratedValue
	private long id;
	
	/**
	 * Empresa que emite el Sobre
	 * 
	 * Es el emisor electronico que opera con este sistema (SobreEmitido)
	 * 
	 * Es un cliente cuando se recibe un sobre (SobreRecibido)
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	protected Empresa empresaEmisora;
	
	/**
	 * Empresa que recibe el Sobre
	 * 
	 * Es DGI cuando se le reportan los sobres (SobreEmitido)
	 * 
	 * Es un cliente cuando se manda el sobre al cliente (SobreEmitido)
	 * 
	 * Es el emisor electronico que opera con este sistema (SobreRecibido)
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	protected Empresa empresaReceptora;
	
	private String nombreArchivo;
	
	private int cantComprobantes;
	
	@Temporal(TemporalType.DATE)
	private Date fecha;
	
	/**
	 * AS - Sobre Recibido 
	 * 
	 * BS - Sobre Rechazado
	 * 
	 * En caso de SobreEmitido este campo es el estado de la respuesta al sobre enviado a la Empresa
	 * 
	 * En caso de SobreRecibido este campo es la respuesta que da el sistema cuando toma el sobre de un correo.
	 */
	@Enumerated(EnumType.STRING)
	private EstadoACKSobreType estadoEmpresa;
	
	/**
	 * Si el estado == BS debe existir un motivo de rechazo
	 * Este motivo representa el motivo del rechazo
	 */
	@Enumerated(EnumType.STRING)
	private MotivoRechazoSobre motivo;
	
	
	/**
	 * Sobre Emitido:
	 * Es el xml que se envia hacia la empresa receptora
	 * 
	 * Sobre Recibido:
	 * Es el xml que se recibe de la empresa emisora
	 */
	@Type(type="text")
	private String xmlEmpresa;

	/**
	 * Sobre recibido cuya respuesta a los CFE (ME_) todavia no se envio: la envia RespuestaCfePoller cuando se cumple el
	 * plazo de la empresa (Empresa.diasRespuestaCfe) o alguien a mano. Los sobres anteriores a esta funcionalidad quedan en
	 * null y nunca se responden solos.
	 */
	private Boolean respuestaCfePendiente;

	/** Lo que observaron los controles de recepcion del sobre (ControlesSobre), en modo log o enforce */
	@Type(type="text")
	private String observacionesRecepcion;
	

//	@Type(type="text")
//	private String respuesta_empresa;
	
	
//	@Type(type="text")
//	private String resultado_empresa;
	
	@ApiModelProperty(hidden = true)
	@Transient
	EnvioCFEEntreEmpresas envioCFEEntreEmpresas;
	
	@ApiModelProperty(hidden = true)
	@Transient
	ACKSobredefType ackSobredefType;
	
	/**
	 * Id que le otorga el emisor al sobre
	 */
	private Long idEmisor;
	
	/** 
	 * 
	 * Email de referencia. Tipicamente es una lista de un solo elemento, 
	 * pero como pueden haber retransmisiones o procesamientos duplicados del mismo 
	 * sobre se modela como lista  
	 * 
	 * SobreEmitido:
	 * 
	 * Es es ack de la empresa
	 * 
	 * SobreRecibido:
	 * 
	 * Es el email en el que vino el sobre
	 */
	@ApiModelProperty(hidden = true)
	@OneToMany
	private List<EmailMessage> emails;

	/**
	 * Respuesta de recepcion y aceptacion del sobre, no implica que los CFE son aceptados.
	 * 
	 * SobreEmitido:
	 * 
	 * Se recibe por mail de parte de la empresa receptora.
	 * 
	 * SobreRecibido:
	 * 
	 * Se genera por el sistema automaticamente si el formato es correcto.
	 * 
	 */
	@OneToOne
	private Respuesta respuestaSobre;

	/** 
	 * 
	 * Resultado de Procesar los CFEs dentro del sobre.
	 * 
	 * SobreEmitido:
	 * 
	 * El resultado proviene de la empresa receptora
	 * 
	 * SobreRecibido:
	 * 
	 * El resultado proviene de la empresa de este sistema, 
	 * este proceso requiere un intervencion manual del usuario.
	 */
	@OneToOne
	private Respuesta respuestaCfes;
	
	public Sobre() {
		super();
	}

	public Sobre(Empresa empresaEmisora, Empresa empresaReceptora, String nombreArchivo, int cantComprobantes) {
		super();
		this.empresaEmisora = empresaEmisora;
		this.empresaReceptora = empresaReceptora;
		this.nombreArchivo = nombreArchivo;
		this.cantComprobantes = cantComprobantes;
	}
	
	protected static Finder<Long, Sobre> find = new Finder<Long, Sobre>(Long.class, Sobre.class);
	
	public static Sobre findById(Long id) {
		return find.byId(id);
	}

	public static Sobre findById(Long id, boolean throwExceptionWhenMissing) throws APIException {
		Sobre sobre = find.byId(id);

		if (sobre == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO).withParams("id", id);
		return sobre;
	}
	
	// Finder sin tipar: las subclases extienden Model<Sobre>, no Model<SobreEmitido>, y no cumplen la cota de Finder
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Finder findEmitidos = new Finder(Long.class, SobreEmitido.class);
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Finder findRecibidos = new Finder(Long.class, SobreRecibido.class);

	/**
	 * Sobres de una empresa, del mas nuevo al mas viejo: los que emitio (EMITIDO) o los que recibio (RECIBIDO).
	 * 
	 * @param estado
	 *            AS, BS o "SIN" (sin estado); null = todos. En emitidos es el estado de DGI, en recibidos el que respondio
	 *            la empresa
	 * @param desde
	 *            fecha del sobre mayor o igual; null = sin filtro
	 * @param hasta
	 *            fecha del sobre menor o igual; null = sin filtro
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Tuple<List<Sobre>, Long> findByEmpresa(Empresa empresa, DireccionDocumento direccion, String estado, Date desde,
			Date hasta, int page, int pageSize) {
		Filtros filtros = new Filtros();
		// estado: el de DGI en los emitidos, la respuesta al sobre (M) en los recibidos
		if (direccion == DireccionDocumento.EMITIDO)
			filtros.estadoDgi = estado;
		else
			filtros.respuestaM = estado;
		return findByEmpresa(empresa, direccion, filtros, desde, hasta, page, pageSize);
	}

	/** Filtros opcionales del listado de sobres (null o vacio = no filtra) */
	public static class Filtros {
		/** Emitidos: estado de DGI (AS, BS, SIN) */
		public String estadoDgi;
		/** Respuesta al sobre entre empresas, M (AS, BS, SIN; en emitidos tambien NO_CORRESPONDE: nunca se le envio) */
		public String respuestaM;
		/** Recibidos: respuesta a los CFE, ME (PENDIENTE, ENVIADA, SIN) */
		public String respuestaME;
		/** RUT de la contraparte (receptor en emitidos, emisor en recibidos), por prefijo */
		public String rutContraparte;
		/** Razon social de la contraparte, contiene (sin distinguir mayusculas) */
		public String razonContraparte;
	}

	private static boolean hay(String valor) {
		return valor != null && !valor.trim().isEmpty();
	}

	/** Estado de sobre (AS/BS) o SIN (null) sobre un campo */
	private static void filtrarEstado(DefaultQuery q, String campo, String valor) {
		if ("SIN".equalsIgnoreCase(valor.trim()))
			q.getCriteria().add(Restrictions.isNull(campo));
		else
			q.getCriteria().add(Restrictions.eq(campo, EstadoACKSobreType.valueOf(valor.trim().toUpperCase())));
	}

	public static Tuple<List<Sobre>, Long> findByEmpresa(Empresa empresa, DireccionDocumento direccion, Filtros filtros, Date desde,
			Date hasta, int page, int pageSize) {
		boolean emitidos = direccion == DireccionDocumento.EMITIDO;
		DefaultQuery q = (DefaultQuery) (emitidos ? findEmitidos.query() : findRecibidos.query());

		q.getCriteria().add(Restrictions.eq(emitidos ? "empresaEmisora" : "empresaReceptora", empresa));

		Filtros f = filtros != null ? filtros : new Filtros();
		if (emitidos && hay(f.estadoDgi))
			filtrarEstado(q, "estadoDgi", f.estadoDgi);
		if (hay(f.respuestaM)) {
			// En los emitidos sin XML para el receptor el sobre nunca se le envio: no corresponde respuesta
			if ("NO_CORRESPONDE".equalsIgnoreCase(f.respuestaM.trim()))
				q.getCriteria().add(Restrictions.isNull("xmlEmpresa"));
			else {
				if (emitidos)
					q.getCriteria().add(Restrictions.isNotNull("xmlEmpresa"));
				filtrarEstado(q, "estadoEmpresa", f.respuestaM);
			}
		}
		if (!emitidos && hay(f.respuestaME)) {
			String me = f.respuestaME.trim().toUpperCase();
			if (me.equals("PENDIENTE"))
				q.getCriteria().add(Restrictions.eq("respuestaCfePendiente", Boolean.TRUE));
			else if (me.equals("ENVIADA"))
				q.getCriteria().add(Restrictions.isNotNull("respuestaCfes"));
			else {
				q.getCriteria().add(Restrictions.isNull("respuestaCfes"));
				q.getCriteria().add(Restrictions.or(Restrictions.isNull("respuestaCfePendiente"), Restrictions.eq("respuestaCfePendiente", Boolean.FALSE)));
			}
		}
		if (hay(f.rutContraparte) || hay(f.razonContraparte)) {
			q.getCriteria().createAlias(emitidos ? "empresaReceptora" : "empresaEmisora", "contraparte");
			if (hay(f.rutContraparte))
				q.getCriteria().add(Restrictions.like("contraparte.rut", f.rutContraparte.trim() + "%"));
			if (hay(f.razonContraparte))
				q.getCriteria().add(Restrictions.ilike("contraparte.razon", "%" + f.razonContraparte.trim() + "%"));
		}

		if (desde != null)
			q.getCriteria().add(Restrictions.ge("fecha", desde));
		if (hasta != null)
			q.getCriteria().add(Restrictions.le("fecha", hasta));

		long rowCount = q.findRowCount();

		q.orderByDesc("fecha");
		q.orderByDesc("id");

		return new Tuple<List<Sobre>, Long>(q.findPage(page, pageSize), rowCount);
	}

	public static List<Sobre> findByNombre(String name) {
		DefaultQuery<Sobre> q = (DefaultQuery<Sobre>) find.query();
		
		q.getCriteria().add(Restrictions.eq("nombreArchivo", name));
		
		return q.findList();
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Empresa getEmpresaEmisora() {
		return empresaEmisora;
	}

	public void setEmpresaEmisora(Empresa empresaEmisora) {
		this.empresaEmisora = empresaEmisora;
	}

	public Empresa getEmpresaReceptora() {
		return empresaReceptora;
	}

	public void setEmpresaReceptora(Empresa empresaReceptora) {
		this.empresaReceptora = empresaReceptora;
	}

	public String getNombreArchivo() {
		if (nombreArchivo==null || nombreArchivo.equals(""))
			nombreArchivo =  FORMATO_NOMBRE_SOBRE.replace("<RUC>", getEmpresaEmisora().getRut()).replace("<FECHA>", formatoFechaSobre.format(new Date())).replace("<ID_EMISOR>", String.valueOf(getId()));
		return nombreArchivo;
	}

	public void setNombreArchivo(String nombreArchivo) {
		this.nombreArchivo = nombreArchivo;
	}

	public int getCantComprobantes() {
		return cantComprobantes;
	}

	public void setCantComprobantes(int cantComprobantes) {
		this.cantComprobantes = cantComprobantes;
	}

	public EstadoACKSobreType getEstadoEmpresa() {
		return estadoEmpresa;
	}

	public void setEstadoEmpresa(EstadoACKSobreType estado) {
		this.estadoEmpresa = estado;
	}

	public MotivoRechazoSobre getMotivo() {
		return motivo;
	}

	public void setMotivo(MotivoRechazoSobre motivo) {
		this.motivo = motivo;
	}

//	public String getRespuesta_empresa() {
//		return respuesta_empresa;
//	}
//
//	public void setRespuesta_empresa(String respuesta_empresa) {
//		this.respuesta_empresa = respuesta_empresa;
//	}
//
//	public String getResultado_empresa() {
//		return resultado_empresa;
//	}
//
//	public void setResultado_empresa(String resultado_empresa) {
//		this.resultado_empresa = resultado_empresa;
//	}

	public String getXmlEmpresa() {
		return xmlEmpresa;
	}

	public boolean isRespuestaCfePendiente() {
		return Boolean.TRUE.equals(respuestaCfePendiente);
	}

	public void setRespuestaCfePendiente(boolean respuestaCfePendiente) {
		this.respuestaCfePendiente = respuestaCfePendiente;
	}

	public String getObservacionesRecepcion() {
		return observacionesRecepcion;
	}

	public void setObservacionesRecepcion(String observacionesRecepcion) {
		this.observacionesRecepcion = observacionesRecepcion;
	}

	public void setXmlEmpresa(String xmlEmpresa) {
		this.xmlEmpresa = xmlEmpresa;
	}
	
	public CFE getCFE(long nro, String serie, TipoDoc tipoDoc) {
		for (Iterator<CFE> iterator = getCfes().iterator(); iterator.hasNext();) {
			CFE cfe = iterator.next();
			if (cfe.getNro()==nro && cfe.getSerie().equals(serie) && cfe.getTipo()==tipoDoc)
				return cfe;
		}
		return null;
		
	}
	
	public abstract List<CFE> getCfes();

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public EnvioCFEEntreEmpresas getEnvioCFEEntreEmpresas() {
		if (envioCFEEntreEmpresas==null)
			try {
				envioCFEEntreEmpresas = (EnvioCFEEntreEmpresas) XML.unMarshall(XML.loadXMLFromString(xmlEmpresa), EnvioCFEEntreEmpresas.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		
		return envioCFEEntreEmpresas;
	}

	public void setEnvioCFEEntreEmpresas(EnvioCFEEntreEmpresas envioCFEEntreEmpresas) {
		this.envioCFEEntreEmpresas = envioCFEEntreEmpresas;
	}

	public ACKSobredefType getAckSobredefType() {
		return ackSobredefType;
	}

	public void setAckSobredefType(ACKSobredefType ackSobredefType) {
		this.ackSobredefType = ackSobredefType;
	}

	public List<EmailMessage> getEmails() {
		if (emails==null)
			emails = new LinkedList<EmailMessage>();

		return emails;
	}

	public void setEmails(List<EmailMessage> emails) {
		this.emails = emails;
	}
	
	public Long getIdEmisor() {
		return idEmisor;
	}

	public void setIdEmisor(Long idEmisor) {
		this.idEmisor = idEmisor;
	}
	
	public Respuesta getRespuestaSobre() {
		return respuestaSobre;
	}


	public void setRespuestaSobre(Respuesta respuestaSobre) {
		this.respuestaSobre = respuestaSobre;
	}


	public Respuesta getRespuestaCfes() {
		return respuestaCfes;
	}


	public void setRespuestaCfes(Respuesta respuestaCfes) {
		this.respuestaCfes = respuestaCfes;
	}
	
}