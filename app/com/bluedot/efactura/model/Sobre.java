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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Restrictions;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.utils.XML;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.sobre.ACKSobredefType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;


@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
@MappedSuperclass
public class Sobre extends Model<Sobre> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8050514407296677614L;

	static final String FORMATO_NOMBRE_SOBRE = "Sob_<RUC>_<FECHA>_<ID>.xml";
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
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "sobre", fetch = FetchType.LAZY)
	private List<CFE> cfes;
	
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
	@Type(type="text")
	private String respuesta_empresa;
	
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
	@Type(type="text")
	private String resultado_empresa;
	
	@Transient
	EnvioCFEEntreEmpresas envioCFEEntreEmpresas;
	
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
	@OneToMany
	private List<EmailMessage> emails;
	
	public Sobre() {
		super();
	}

	public Sobre(Empresa empresaEmisora, Empresa empresaReceptora, String nombreArchivo, int cantComprobantes,
			List<CFE> cfes) {
		super();
		this.empresaEmisora = empresaEmisora;
		this.empresaReceptora = empresaReceptora;
		this.nombreArchivo = nombreArchivo;
		this.cantComprobantes = cantComprobantes;
		this.cfes = cfes;
	}
	
	protected static Finder<Long, Sobre> find = new Finder<Long, Sobre>(Long.class, Sobre.class);
	
	public static Sobre findById(Long id) {
		return find.byId(id);
	}

	public static Sobre findById(Long id, boolean throwExceptionWhenMissing) throws APIException {
		Sobre sobre = find.byId(id);

		if (sobre == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.SOBRE_NO_ENCONTRADO.withParams("id", id));
		return sobre;
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
			nombreArchivo =  FORMATO_NOMBRE_SOBRE.replace("<RUC>", getEmpresaEmisora().getRut()).replace("<FECHA>", formatoFechaSobre.format(new Date())).replace("<ID>", String.valueOf(getId()));
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

	public List<CFE> getCfes() {
		if (cfes==null)
			cfes = new LinkedList<CFE>();
		return cfes;
	}

	public void setCfes(List<CFE> cfes) {
		this.cfes = cfes;
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

	public String getRespuesta_empresa() {
		return respuesta_empresa;
	}

	public void setRespuesta_empresa(String respuesta_empresa) {
		this.respuesta_empresa = respuesta_empresa;
	}

	public String getResultado_empresa() {
		return resultado_empresa;
	}

	public void setResultado_empresa(String resultado_empresa) {
		this.resultado_empresa = resultado_empresa;
	}

	public String getXmlEmpresa() {
		return xmlEmpresa;
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
	
}