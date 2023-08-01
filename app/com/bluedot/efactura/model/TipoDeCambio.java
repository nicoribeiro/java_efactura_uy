package com.bluedot.efactura.model;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;
import dgi.classes.recepcion.TipMonType;
import org.hibernate.criterion.Restrictions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.*;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;


@Entity
public class TipoDeCambio extends Model<TipoDeCambio> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	final static Logger logger = LoggerFactory.getLogger(TipoDeCambio.class);	
	
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "tipo_de_cambio_seq")
	@SequenceGenerator(name = "tipo_de_cambio_seq", sequenceName = "tipo_de_cambio_seq" )
	private long id;
	@Temporal(TemporalType.DATE)
	private Date fecha;
	@Enumerated(EnumType.STRING)
	private TipMonType moneda;
	private BigDecimal compra;
	private BigDecimal venta;
	private BigDecimal interbancario;
	

	public TipoDeCambio() {
	}
	
	public TipoDeCambio(Date fecha, TipMonType moneda, BigDecimal compra, BigDecimal venta) {
		super();
		this.fecha = fecha;
		this.moneda = moneda;
		this.compra = compra;
		this.venta = venta;
	}

	private static Finder<Long, TipoDeCambio> find = new Finder<Long, TipoDeCambio>(Long.class, TipoDeCambio.class);

	public static TipoDeCambio findByFechaYMoneda(Date fecha, TipMonType moneda) {
		DefaultQuery<TipoDeCambio> q = (DefaultQuery<TipoDeCambio>) find.query();

		q.getCriteria().add(Restrictions.eq("moneda", moneda));
		q.getCriteria().add(Restrictions.eq("fecha", fecha));

		TipoDeCambio tc =  q.findUnique();
		return tc;
		
	}
	
	public static TipoDeCambio findByFechaYMoneda(Date fecha, TipMonType moneda, boolean throwExceptionWhenMissing) throws APIException {
		TipoDeCambio tc = findByFechaYMoneda(fecha, moneda);
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		
		if (tc == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.TIPO_CAMBIO_NO_ENCONTRADO).withParams("fecha", sdf.format(fecha), "Tipo", moneda.name());
		
		return tc;

	}

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
	}

	public TipMonType getMoneda() {
		return moneda;
	}

	public void setMoneda(TipMonType moneda) {
		this.moneda = moneda;
	}

	public BigDecimal getCompra() {
		return compra;
	}

	public void setCompra(BigDecimal compra) {
		this.compra = compra;
	}

	public BigDecimal getVenta() {
		return venta;
	}

	public void setVenta(BigDecimal venta) {
		this.venta = venta;
	}

	public BigDecimal getInterbancario() {
		return interbancario;
	}

	public void setInterbancario(BigDecimal interbancario) {
		this.interbancario = interbancario;
	}
}
