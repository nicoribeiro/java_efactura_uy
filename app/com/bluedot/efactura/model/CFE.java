package com.bluedot.efactura.model;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.CascadeType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.utils.Tuple;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.ERem;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
@ApiModel
public class CFE extends Model<CFE>{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 2833574192837235368L;

	@Id
	@GeneratedValue
	private long id;
	
	/**
	 * Empresa que emite el CFE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Empresa empresaEmisora;
	
	/**
	 * Empresa que recibe el CFE
	 * 
	 * Si este campo esta presente => titular=null;
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Empresa empresaReceptora;
	
	/**
	 * Titular que recibe el CFE
	 * 
	 * Si este campo esta presente => empresaReceptora=null
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private Titular titular;
	
	/**
	 * Sobre que contiene el CFE cuando es emitido por el Sistema
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private SobreEmitido sobreEmitido;
	
	/**
	 * Sobre que contiene el CFE cuando fue recibido por el sistema
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private SobreRecibido sobreRecibido;
	
	/**
	 * Reporte diario en el que se reporto el CFE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private ReporteDiario reporteDiario;
	
	/**
	 * Nro de ordinal dentro del sobre
	 */
	private int ordinal;
	
	@Enumerated(EnumType.STRING)
	private TipoDoc tipo;
	
	private String serie;
	
	private long nro;
	
	@Temporal(TemporalType.DATE)
	private Date fechaEmision;
	
	@Temporal(TemporalType.DATE)
	private Date fechaGeneracion;
	
	/**
	 * Periodo para servicios
	 */
	private Date facturadoDesde;
	
	/**
	 * Periodo para servicios;
	 */
	private Date facturadoHasta;
	
	/**
	 * Líneas de detalle si expresan en burto (IVA incluido)
	 */
	private boolean indMontoBruto;
	
	/**
	 * Forma de pago
	 */
	@Enumerated(EnumType.STRING)
	private FormaDePago formaDePago;
	
	private Date vencimiento;
	
	@Enumerated(EnumType.STRING)
	private TipMonType moneda;
	
	/**
	 * Si la moneda es distinta de UYU este campo debe expresar el tipo de cambio
	 */
	private double tipoCambio;
	
	/**
	 * Todos los totales que la DGI pide en un CFE
	 * 
	 */
	private Double totMntNoGrv; 
	private Double totMntExpyAsim; 
	private Double totMntImpPerc; 
	private Double totMntIVAenSusp; 
	private Double totMntIVATasaMin; 
	private Double totMntIVATasaBas; 
	private Double totMntIVAOtra; 
	private Double mntIVATasaMin; 
	private Double mntIVATasaBas; 
	private Double mntIVAOtra; 
	private Double ivaTasaMin;	
	private Double ivaTasaBas; 
	private Double totMntTotal; 
	private Double totMntRetenido; 
	private Double totValRetPerc; 
	
	/**
	 * Cantidad de lineas, cantlineas=detalle.length()
	 */
	private int cantLineas;
	
	/**
	 * Hash del comprobante
	 */
	private String hash;
	
	/**
	 * AE - Comprobante Recibido
	 * 
	 * BE - Comprobante Rechazado (CFE) 
	 * 
	 * CE - Comprobante Observado (CFC) (uso exclusivo DGI)
	 */
	@Enumerated(EnumType.STRING)
	private EstadoACKCFEType estado;
	
	/**
	 * Si el estado == BE || estado == CE entonces debe de haver un motivo.
	 * Este campo representa el motivo de rechazo
	 */
	@ElementCollection
	@Enumerated(EnumType.STRING)
	private List<MotivoRechazoCFE> motivo;
	
	/**
	 * Lineas del CFE
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy="cfe", fetch=FetchType.LAZY)
	private List<Detalle> detalle;

	@ApiModelProperty(hidden = true)
	@Transient
	private EFact efactura;
	
	@ApiModelProperty(hidden = true)
	@Transient
	private ETck eticket;
	
	@ApiModelProperty(hidden = true)
	@Transient
	private EResg eresguardo;
	
	@ApiModelProperty(hidden = true)
	@Transient
	private ERem eremito;
	
	@Type(type="text")
	private String generadorJson;
	
	private String generadorId;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	private Sucursal sucursal;
	
	/**
	 * CAE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private CAE cae;
	
	@ApiModelProperty(hidden = true)
	@Lob
	private byte[] qr;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private CFE referencia;
	
	private String razonReferencia;
	
	@Type(type="text")
	private String adenda;

	@OneToMany
	private List<RetencionPercepcion> retencionesPercepciones; 
	
	private String pdfMailAddress;
	
	public CFE() {
		super();
		setFechaGeneracion(new Date());
	}
	
	private static Finder<Integer, CFE> find = new Finder<Integer, CFE>(Integer.class, CFE.class);
	
	public static CFE findById(Integer id) {
		return find.byId(id);
	}

	public static CFE findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		CFE cfe = find.byId(id);

		if (cfe == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("id", id);
		return cfe;
	}
	
	public static List<CFE> findById(Empresa empresa, TipoDoc tipo, String serie, long nro, EstadoACKCFEType estado, DireccionDocumento direccion, boolean throwExceptionWhenMissing) throws APIException
	{
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();
		
		switch (direccion) {
		case AMBOS:
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("DireccionDocumento", direccion.name());
		case EMITIDO:
			q.getCriteria().add(Restrictions.eq("empresaEmisora", empresa));
			q.getCriteria().add(Restrictions.isNotNull("sobreEmitido"));
			break;
		case RECIBIDO:
			q.getCriteria().add(Restrictions.eq("empresaReceptora", empresa));
			q.getCriteria().add(Restrictions.isNotNull("sobreRecibido"));
			break;
		}
			
			
		q.getCriteria().add(Restrictions.and
		(		 
			Restrictions.eq("tipo",tipo), 
			Restrictions.eq("serie", serie), 
			Restrictions.eq("nro", nro)
		));
		
		if (estado!=null)
			q.getCriteria().add(Restrictions.eq("estado", estado));
		
		List<CFE> cfe =  q.findList();
		if ((cfe == null || cfe.size()==0)&& throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("tipo-serie-nro", tipo.value+"-"+serie+"-"+nro);
		return cfe;
	}
	
	public static CFE findByGeneradorId(Empresa empresa, String id, DireccionDocumento direccion) throws APIException {
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();

		switch (direccion) {
		case AMBOS:
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("DireccionDocumento", direccion.name());
		case EMITIDO:
			q.getCriteria().add(Restrictions.eq("empresaEmisora", empresa));
			q.getCriteria().add(Restrictions.isNotNull("sobreEmitido"));
			break;
		case RECIBIDO:
			q.getCriteria().add(Restrictions.eq("empresaReceptora", empresa));
			q.getCriteria().add(Restrictions.isNotNull("sobreRecibido"));
			break;
		}
		
		q.getCriteria().add(Restrictions.eq("generadorId", id));

		CFE cfe = q.findUnique();
		return cfe;
	}
	
	public static List<CFE> findByEmpresaEmisoraAndFechaGeneracion(Empresa empresaEmisora, Date fechaGeneracion) throws APIException
	{
		return findByEmpresaEmisoraAndFechaGeneracion(empresaEmisora, fechaGeneracion, false);
	}
	
	public static List<CFE> findByEmpresaEmisoraAndFechaGeneracion(Empresa empresaEmisora, Date fechaGeneracion, boolean soloEmitidos) throws APIException
	{
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();
			
			q.getCriteria().createAlias("empresaEmisora", "empresaEmis", JoinType.LEFT_OUTER_JOIN);
			
			q.getCriteria().add(Restrictions.and
					
					(		Restrictions.eq("empresaEmis.id", empresaEmisora.getId()), 
							Restrictions.eq("fechaGeneracion", fechaGeneracion)
					));
			/*
			 * Esto filtra los CFE que la empresaEmisora = empresaReceptora (facturas autoemitidas)
			 */
			if (soloEmitidos)
				q.getCriteria().add(Restrictions.isNull("sobreRecibido"));
			
		
		List<CFE> cfes = q.findList();
		
		return cfes;
		
	}
	
	
	public static Tuple<List<CFE>,Long> find(Empresa empresa, Date desdeFechaEmision, Date hastaFechaEmision, int page, int pageSize, DireccionDocumento direccion)
	{
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();

		Criterion dateCriteria = null;
		
		if (desdeFechaEmision != null)
			if (hastaFechaEmision==null)
				dateCriteria = Restrictions.ge("fechaEmision", desdeFechaEmision);
			else
				dateCriteria = Restrictions.between("fechaEmision", desdeFechaEmision, hastaFechaEmision);
		else
			if (hastaFechaEmision!=null)
				dateCriteria = Restrictions.le("fechaEmision", hastaFechaEmision);
		
		if (dateCriteria!=null)
			q.getCriteria().add(dateCriteria);
		
		switch (direccion) {
		case AMBOS:
			q.getCriteria().add( Restrictions.or( Restrictions.eq("empresaReceptora", empresa), Restrictions.eq("empresaEmisora", empresa))   );
			break;
		case EMITIDO:
			q.getCriteria().add(Restrictions.eq("empresaEmisora", empresa)  );
			break;
		case RECIBIDO:
			q.getCriteria().add( Restrictions.eq("empresaReceptora", empresa));
			break;
		}
		
		long rowCount = q.findRowCount();
		
		//Sort by id by default
		q.orderByAsc("id");
		
		List<CFE> list =  page > 0 && pageSize > 0 ? q.findPage(page, pageSize) : q.findList();
		
		return new Tuple<List<CFE>, Long>(list, rowCount);
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

	public Titular getTitular() {
		return titular;
	}

	public void setTitular(Titular titular) {
		this.titular = titular;
	}

	public SobreEmitido getSobreEmitido() {
		return sobreEmitido;
	}

	public void setSobreEmitido(SobreEmitido sobreEmitido) {
		this.sobreEmitido = sobreEmitido;
	}

	public ReporteDiario getReporteDiario() {
		return reporteDiario;
	}

	public void setReporteDiario(ReporteDiario reporteDiario) {
		this.reporteDiario = reporteDiario;
	}

	public int getOrdinal() {
		return ordinal;
	}

	public void setOrdinal(int ordinal) {
		this.ordinal = ordinal;
	}

	public TipoDoc getTipo() {
		return tipo;
	}

	public void setTipo(TipoDoc tipo) {
		this.tipo = tipo;
	}

	public String getSerie() {
		return serie;
	}

	public void setSerie(String serie) {
		this.serie = serie;
	}

	public long getNro() {
		return nro;
	}

	public void setNro(long nro) {
		this.nro = nro;
	}

	public Date getFechaEmision() {
		return fechaEmision;
	}

	public void setFechaEmision(Date fechaEmision) {
		this.fechaEmision = fechaEmision;
	}

	public Date getFacturadoDesde() {
		return facturadoDesde;
	}

	public void setFacturadoDesde(Date facturadoDesde) {
		this.facturadoDesde = facturadoDesde;
	}

	public Date getFacturadoHasta() {
		return facturadoHasta;
	}

	public void setFacturadoHasta(Date facturadoHasta) {
		this.facturadoHasta = facturadoHasta;
	}

	public boolean isIndMontoBruto() {
		return indMontoBruto;
	}

	public void setIndMontoBruto(boolean indMontoBruto) {
		this.indMontoBruto = indMontoBruto;
	}

	public FormaDePago getFormaDePago() {
		return formaDePago;
	}

	public void setFormaDePago(FormaDePago formaDePago) {
		this.formaDePago = formaDePago;
	}

	public Date getVencimiento() {
		return vencimiento;
	}

	public void setVencimiento(Date vencimiento) {
		this.vencimiento = vencimiento;
	}

	public TipMonType getMoneda() {
		return moneda;
	}

	public void setMoneda(TipMonType moneda) {
		this.moneda = moneda;
	}

	public double getTipoCambio() {
		return tipoCambio;
	}

	public void setTipoCambio(double tipoCambio) {
		this.tipoCambio = tipoCambio;
	}

	public int getCantLineas() {
		return cantLineas;
	}

	public void setCantLineas(int cantLineas) {
		this.cantLineas = cantLineas;
	}

	public String getHash() {
		return hash;
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	public EstadoACKCFEType getEstado() {
		return estado;
	}

	public void setEstado(EstadoACKCFEType estado) {
		this.estado = estado;
	}

	public List<Detalle> getDetalle() {
		if (detalle==null)
			detalle = new LinkedList<Detalle>();
		return detalle;
	}

	public void setDetalle(List<Detalle> detalle) {
		this.detalle = detalle;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public EFact getEfactura() {
		return efactura;
	}

	public void setEfactura(EFact efactura) {
		this.efactura = efactura;
	}

	public ETck getEticket() {
		return eticket;
	}

	public void setEticket(ETck eticket) {
		this.eticket = eticket;
	}

	public EResg getEresguardo() {
		return eresguardo;
	}

	public void setEresguardo(EResg eresguardo) {
		this.eresguardo = eresguardo;
	}

	public List<MotivoRechazoCFE> getMotivo() {
		if (motivo==null)
			motivo = new LinkedList<MotivoRechazoCFE>();
		return motivo;
	}

	public void setMotivo(List<MotivoRechazoCFE> motivo) {
		this.motivo = motivo;
	}

	public Double getTotMntNoGrv() {
		return totMntNoGrv;
	}

	public void setTotMntNoGrv(Double totMntNoGrv) {
		this.totMntNoGrv = totMntNoGrv;
	}

	public Double getTotMntExpyAsim() {
		return totMntExpyAsim;
	}

	public void setTotMntExpyAsim(Double totMntExpyAsim) {
		this.totMntExpyAsim = totMntExpyAsim;
	}

	public Double getTotMntImpPerc() {
		return totMntImpPerc;
	}

	public void setTotMntImpPerc(Double totMntImpPerc) {
		this.totMntImpPerc = totMntImpPerc;
	}

	public Double getTotMntIVAenSusp() {
		return totMntIVAenSusp;
	}

	public void setTotMntIVAenSusp(Double totMntIVAenSusp) {
		this.totMntIVAenSusp = totMntIVAenSusp;
	}

	public Double getTotMntIVATasaMin() {
		return totMntIVATasaMin;
	}

	public void setTotMntIVATasaMin(Double totMntIVATasaMin) {
		this.totMntIVATasaMin = totMntIVATasaMin;
	}

	public Double getTotMntIVATasaBas() {
		return totMntIVATasaBas;
	}

	public void setTotMntIVATasaBas(Double totMntIVATasaBas) {
		this.totMntIVATasaBas = totMntIVATasaBas;
	}

	public Double getTotMntIVAOtra() {
		return totMntIVAOtra;
	}

	public void setTotMntIVAOtra(Double totMntIVAOtra) {
		this.totMntIVAOtra = totMntIVAOtra;
	}

	public Double getMntIVATasaMin() {
		return mntIVATasaMin;
	}

	public void setMntIVATasaMin(Double mntIVATasaMin) {
		this.mntIVATasaMin = mntIVATasaMin;
	}

	public Double getMntIVATasaBas() {
		return mntIVATasaBas;
	}

	public void setMntIVATasaBas(Double mntIVATasaBas) {
		this.mntIVATasaBas = mntIVATasaBas;
	}

	public Double getMntIVAOtra() {
		return mntIVAOtra;
	}

	public void setMntIVAOtra(Double mntIVAOtra) {
		this.mntIVAOtra = mntIVAOtra;
	}

	public Double getIvaTasaMin() {
		return ivaTasaMin;
	}

	public void setIvaTasaMin(Double ivaTasaMin) {
		this.ivaTasaMin = ivaTasaMin;
	}

	public Double getIvaTasaBas() {
		return ivaTasaBas;
	}

	public void setIvaTasaBas(Double ivaTasaBas) {
		this.ivaTasaBas = ivaTasaBas;
	}

	public Double getTotMntTotal() {
		return totMntTotal;
	}

	public void setTotMntTotal(Double totMntTotal) {
		this.totMntTotal = totMntTotal;
	}

	public Double getTotMntRetenido() {
		return totMntRetenido;
	}

	public void setTotMntRetenido(Double totMntRetenido) {
		this.totMntRetenido = totMntRetenido;
	}

	public Double getTotValRetPerc() {
		return totValRetPerc;
	}

	public void setTotValRetPerc(Double totValRetPerc) {
		this.totValRetPerc = totValRetPerc;
	}

	public String getGeneradorJson() {
		return generadorJson;
	}

	public void setGeneradorJson(String generadorJson) {
		this.generadorJson = generadorJson;
	}

	public String getGeneradorId() {
		return generadorId;
	}

	public void setGeneradorId(String generadorId) {
		this.generadorId = generadorId;
	}
	
	public CAE getCae() {
		return cae;
	}

	public void setCae(CAE cae) {
		this.cae = cae;
	}
	
	public byte[] getQr() {
		return qr;
	}

	public void setQr(byte[] qr) {
		this.qr = qr;
	}

	public BufferedImage qrAsImage() {
        InputStream in = new ByteArrayInputStream(qr);
        try {
			return ImageIO.read(in);
		} catch (IOException e) {
		}
        return null;
    }

    public void qrAsImage(BufferedImage image) {
        try {
        	ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(image, "PNG" /* for instance */, out);
			qr = out.toByteArray();
		} catch (IOException e) {
		}
    }

	public CFE getReferencia() {
		return referencia;
	}

	public void setReferencia(CFE referencia) {
		this.referencia = referencia;
	}

	public String getRazonReferencia() {
		return razonReferencia;
	}

	public void setRazonReferencia(String razonReferencia) {
		this.razonReferencia = razonReferencia;
	}
	
	public boolean isObligatorioReferencia(){
		switch (getTipo()) {
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			return true;
		default:
			return false;
		}
	}

	public String getAdenda() {
		return adenda;
	}

	public void setAdenda(String adenda) {
		this.adenda = adenda;
	}

	public ERem getEremito() {
		return eremito;
	}

	public void setEremito(ERem eremito) {
		this.eremito = eremito;
	}

	public SobreRecibido getSobreRecibido() {
		return sobreRecibido;
	}

	public void setSobreRecibido(SobreRecibido sobreRecibido) {
		this.sobreRecibido = sobreRecibido;
	}
	
	public List<RetencionPercepcion> getRetencionesPercepciones() {
		if (retencionesPercepciones==null)
			retencionesPercepciones = new LinkedList<RetencionPercepcion>();
		return retencionesPercepciones;
	}

	public void setRetencionesPercepciones(List<RetencionPercepcion> retencionesPercepciones) {
		this.retencionesPercepciones = retencionesPercepciones;
	}

	public Date getFechaGeneracion() {
		return fechaGeneracion;
	}

	public void setFechaGeneracion(Date fechaGeneracion) {
		this.fechaGeneracion = fechaGeneracion;
	}

	public String getPdfMailAddress() {
		return pdfMailAddress;
	}

	public void setPdfMailAddress(String pdfMailAddress) {
		this.pdfMailAddress = pdfMailAddress;
	}

	public Sucursal getSucursal() {
		return sucursal;
	}

	public void setSucursal(Sucursal sucursal) {
		this.sucursal = sucursal;
	}

}
