package com.bluedot.efactura.model;

import com.bluedot.efactura.commons.Orden;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.utils.Tuple;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPA;

/**
 * Registro de cada ejecucion de un proceso (poller) que lo lleva, automatica o pedida a mano: cuando, como termino, un
 * resumen y el detalle (JSON). Se consulta desde el panel (Procesos).
 *
 * Se guarda con un EntityManager propio y su propia transaccion: la ejecucion queda registrada aunque la transaccion del
 * proceso haga rollback por un error.
 */
@Entity
public class EjecucionProceso extends Model<EjecucionProceso> {

	private static final long serialVersionUID = 1L;

	final static Logger logger = LoggerFactory.getLogger(EjecucionProceso.class);

	@Id
	@GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ejecucion_proceso_seq")
	@SequenceGenerator(name = "ejecucion_proceso_seq", sequenceName = "ejecucion_proceso_seq")
	private long id;

	/** Nombre del proceso (el del poller, ej. EmisoresElectronicosPoller) */
	private String proceso;

	@Temporal(TemporalType.TIMESTAMP)
	private Date inicio;

	@Temporal(TemporalType.TIMESTAMP)
	private Date fin;

	private boolean ok;

	/** "automático" o el mail del usuario que la pidio */
	@Column(columnDefinition = "TEXT")
	private String origen;

	@Column(columnDefinition = "TEXT")
	private String resumen;

	/** Detalle de la ejecucion (JSON), o el error */
	@Column(columnDefinition = "TEXT")
	private String detalle;

	public EjecucionProceso() {
	}

	private static Finder<Long, EjecucionProceso> find = new Finder<Long, EjecucionProceso>(Long.class, EjecucionProceso.class);

	/** Registra una ejecucion en su propia transaccion. Nunca falla: si no se puede registrar solo se loguea. */
	public static void registrar(String proceso, Date inicio, boolean ok, String origen, String resumen, String detalle) {
		EntityManager em = null;
		try {
			em = JPA.em("default");
			em.getTransaction().begin();
			EjecucionProceso e = new EjecucionProceso();
			e.proceso = proceso;
			e.inicio = inicio;
			e.fin = new Date();
			e.ok = ok;
			e.origen = origen;
			e.resumen = resumen;
			e.detalle = detalle;
			em.persist(e);
			em.getTransaction().commit();
		} catch (Throwable t) {
			logger.error("No se pudo registrar la ejecucion de " + proceso, t);
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

	/** Campos por los que se pueden ordenar las ejecuciones (ok = resultado) */
	public static final String[] ORDENES = { "inicio", "ok", "origen", "resumen" };

	/** Ejecuciones de un proceso, de la mas nueva a la mas vieja, paginadas; con el total */
	public static Tuple<java.util.List<EjecucionProceso>, Long> findByProceso(String proceso, int page, int pageSize) {
		return findByProceso(proceso, page, pageSize, null);
	}

	/**
	 * @param orden
	 *            uno de {@link #ORDENES}; null = de la mas nueva a la mas vieja
	 */
	public static Tuple<java.util.List<EjecucionProceso>, Long> findByProceso(String proceso, int page, int pageSize, Orden orden) {
		DefaultQuery<EjecucionProceso> q = (DefaultQuery<EjecucionProceso>) find.query();
		q.getCriteria().add(Restrictions.eq("proceso", proceso));
		long total = q.findRowCount();
		if (orden == null) {
			q.getCriteria().addOrder(Order.desc("inicio"));
			q.getCriteria().addOrder(Order.desc("id"));
		} else
			orden.aplicar(q, orden.campo);
		return new Tuple<java.util.List<EjecucionProceso>, Long>(q.findPage(page, pageSize), total);
	}

	public long getId() {
		return id;
	}

	public String getProceso() {
		return proceso;
	}

	public Date getInicio() {
		return inicio;
	}

	public Date getFin() {
		return fin;
	}

	public boolean isOk() {
		return ok;
	}

	public String getOrigen() {
		return origen;
	}

	public String getResumen() {
		return resumen;
	}

	public String getDetalle() {
		return detalle;
	}
}
