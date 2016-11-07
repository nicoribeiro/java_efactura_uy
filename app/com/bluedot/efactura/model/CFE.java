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
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;

@Entity
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
	 * Sobre que contiene el CFE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private SobreEmitido sobre;
	
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
	private Date fecha;
	
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
	 * El CFE en formato xml
	 */
	@Type(type="text")
	private String xml;
	
	/**
	 * Lineas del CFE
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy="cfe", fetch=FetchType.LAZY)
	private List<Detalle> detalle;

	@Transient
	private EFact efactura;
	
	@Transient
	private ETck eticket;
	
	@Transient
	private EResg eresguardo;
	
	@Type(type="text")
	private String generadorJson;
	
	private String generadorId;
	
	/**
	 * CAE
	 */
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private CAE cae;
	
	@Lob
	private byte[] qr;
	
	@ManyToOne(cascade = CascadeType.ALL, fetch=FetchType.LAZY)
	private CFE referencia;
	
	private String razonReferencia;
	
	@Type(type="text")
	private String adenda;

	public CFE() {
		super();
	}

	public CFE(Empresa empresaEmisora, TipoDoc tipo, String serie, long nro, Date fecha, boolean indMontoBruto,
			FormaDePago formaDePago, TipMonType moneda, int cantLineas) {
		super();
		this.empresaEmisora = empresaEmisora;
		this.tipo = tipo;
		this.serie = serie;
		this.nro = nro;
		this.fecha = fecha;
		this.indMontoBruto = indMontoBruto;
		this.formaDePago = formaDePago;
		this.moneda = moneda;
		this.cantLineas = cantLineas;
	}
	
	private static Finder<Integer, CFE> find = new Finder<Integer, CFE>(Integer.class, CFE.class);
	
	public static CFE findById(Integer id) {
		return find.byId(id);
	}

	public static CFE findById(Integer id, boolean throwExceptionWhenMissing) throws APIException {
		CFE cfe = find.byId(id);

		if (cfe == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO.withParams("id", id));
		return cfe;
	}

	
	public static CFE findById(Empresa empresa, TipoDoc tipo, String serie, long nro, boolean throwExceptionWhenMissing) throws APIException
	{
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();
		
			
			q.getCriteria().createAlias("empresaEmisora", "empresa", JoinType.LEFT_OUTER_JOIN);
			
			q.getCriteria().add(Restrictions.and
					
					(		Restrictions.eq("empresa.id", empresa.getId()), 
							Restrictions.eq("tipo",tipo), 
							Restrictions.eq("serie", serie), 
							Restrictions.eq("nro", nro)
					));
		
		CFE cfe =  q.findUnique();
		if (cfe == null && throwExceptionWhenMissing)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO.withParams("tipo-serie-nro", tipo.value+"-"+serie+"-"+nro));
		return cfe;
	}
	
	public static CFE findByGeneradorId(Empresa empresa, String id) {
		DefaultQuery<CFE> q = (DefaultQuery<CFE>) find.query();

		q.getCriteria().add(Restrictions.eq("generadorId", id));

		CFE cfe = q.findUnique();
		return cfe;
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

	public SobreEmitido getSobre() {
		return sobre;
	}

	public void setSobre(SobreEmitido sobre) {
		this.sobre = sobre;
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

	public Date getFecha() {
		return fecha;
	}

	public void setFecha(Date fecha) {
		this.fecha = fecha;
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

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
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

	public BufferedImage getQrAsImage() {
        InputStream in = new ByteArrayInputStream(qr);
        try {
			return ImageIO.read(in);
		} catch (IOException e) {
		}
        return null;
    }

    public void setQrAsImage(BufferedImage image) {
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

}
