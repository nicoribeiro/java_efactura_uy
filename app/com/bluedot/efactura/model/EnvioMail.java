package com.bluedot.efactura.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPA;

/**
 * Registro de cada intento de envio de un mail (exitoso o no): XML y PDF de los comprobantes a los receptores, ACK a los
 * emisores de los sobres recibidos y reenvios manuales.
 *
 * Se guarda con un EntityManager propio y su propia transaccion: el intento queda registrado aunque la operacion que
 * envio el mail falle y se haga rollback.
 */
@Entity
public class EnvioMail extends Model<EnvioMail> {

	private static final long serialVersionUID = 1L;

	final static Logger logger = LoggerFactory.getLogger(EnvioMail.class);

	public enum Tipo {
		/** XML del comprobante al receptor electronico */
		XML_RECEPTOR,
		/** PDF del comprobante */
		PDF,
		/** Respuesta (ACK) al emisor de un sobre recibido */
		ACK_SOBRE,
		/** Respuesta (ACK) al emisor por los CFE de un sobre recibido */
		ACK_CFE,
		OTRO
	}

	/** Lo que se sabe de un envio ademas del mail en si: para que es y de que comprobante o sobre */
	public static class Contexto {
		public final Tipo tipo;
		/** "automatico" o el mail del usuario que lo pidio */
		public final String origen;
		public Integer tipoDoc;
		public String serie;
		public Long nro;
		public Long sobreId;

		public Contexto(Tipo tipo, String origen) {
			this.tipo = tipo;
			this.origen = origen;
		}

		public static Contexto automatico(Tipo tipo) {
			return new Contexto(tipo, "automático");
		}

		public Contexto comprobante(CFE cfe) {
			if (cfe != null) {
				tipoDoc = cfe.getTipo().value;
				serie = cfe.getSerie();
				nro = cfe.getNro();
			}
			return this;
		}

		public Contexto sobre(Long sobreId) {
			this.sobreId = sobreId;
			return this;
		}
	}

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "envio_mail_seq")
	@SequenceGenerator(name = "envio_mail_seq", sequenceName = "envio_mail_seq")
	private long id;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fecha;

	/** Empresa que envia el mail */
	@ManyToOne(fetch = FetchType.LAZY)
	private Empresa empresa;

	@Column(columnDefinition = "TEXT")
	private String destinatarios;

	@Column(columnDefinition = "TEXT")
	private String archivo;

	@Column(columnDefinition = "TEXT")
	private String asunto;

	@Enumerated(EnumType.STRING)
	private Tipo tipo;

	private boolean ok;

	@Column(columnDefinition = "TEXT")
	private String error;

	@Column(columnDefinition = "TEXT")
	private String origen;

	private Integer tipoDoc;

	private String serie;

	private Long nro;

	private Long sobreId;

	public EnvioMail() {
	}

	private static Finder<Long, EnvioMail> find = new Finder<Long, EnvioMail>(Long.class, EnvioMail.class);

	/**
	 * Registra un intento de envio en su propia transaccion. Nunca falla: si no se puede registrar solo se loguea, para no
	 * afectar el envio.
	 */
	public static void registrar(Empresa empresa, String destinatarios, String archivo, String asunto, Contexto contexto,
			Throwable error) {
		EntityManager em = null;
		try {
			em = JPA.em("default");
			em.getTransaction().begin();
			EnvioMail envio = new EnvioMail();
			envio.fecha = new Date();
			envio.empresa = empresa != null ? em.getReference(Empresa.class, empresa.getId()) : null;
			envio.destinatarios = destinatarios;
			envio.archivo = archivo;
			envio.asunto = asunto;
			Contexto c = contexto != null ? contexto : Contexto.automatico(Tipo.OTRO);
			envio.tipo = c.tipo;
			envio.origen = c.origen;
			envio.tipoDoc = c.tipoDoc;
			envio.serie = c.serie;
			envio.nro = c.nro;
			envio.sobreId = c.sobreId;
			envio.ok = error == null;
			if (error != null)
				envio.error = error.getMessage() != null ? error.getMessage() : error.getClass().getName();
			em.persist(envio);
			em.getTransaction().commit();
		} catch (Throwable t) {
			logger.error("No se pudo registrar el envio de mail a " + destinatarios, t);
			try {
				if (em != null && em.getTransaction().isActive())
					em.getTransaction().rollback();
			} catch (Throwable ignorado) {
			}
		} finally {
			if (em != null)
				em.close();
		}
	}

	/** Envios de un comprobante emitido por la empresa, del mas nuevo al mas viejo */
	public static List<EnvioMail> findByComprobante(Empresa empresa, int tipoDoc, String serie, long nro) {
		DefaultQuery<EnvioMail> q = (DefaultQuery<EnvioMail>) find.query();
		q.getCriteria().add(Restrictions.eq("empresa", empresa));
		q.getCriteria().add(Restrictions.eq("tipoDoc", tipoDoc));
		q.getCriteria().add(Restrictions.eq("serie", serie));
		q.getCriteria().add(Restrictions.eq("nro", nro));
		q.orderByDesc("fecha");
		return q.findList();
	}

	/** Envios relacionados con un sobre (ACK de los recibidos), del mas nuevo al mas viejo */
	public static List<EnvioMail> findBySobre(Empresa empresa, long sobreId) {
		DefaultQuery<EnvioMail> q = (DefaultQuery<EnvioMail>) find.query();
		q.getCriteria().add(Restrictions.eq("empresa", empresa));
		q.getCriteria().add(Restrictions.eq("sobreId", sobreId));
		q.orderByDesc("fecha");
		return q.findList();
	}

	public long getId() {
		return id;
	}

	public Date getFecha() {
		return fecha;
	}

	public String getDestinatarios() {
		return destinatarios;
	}

	public String getArchivo() {
		return archivo;
	}

	public String getAsunto() {
		return asunto;
	}

	public Tipo getTipo() {
		return tipo;
	}

	public boolean isOk() {
		return ok;
	}

	public String getError() {
		return error;
	}

	public String getOrigen() {
		return origen;
	}

	public Long getSobreId() {
		return sobreId;
	}
}
