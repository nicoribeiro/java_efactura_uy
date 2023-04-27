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

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.EmailMessage;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@Entity
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
	
	private String mailNotificaciones;
	
	private String fromEnvio; 
	
	private int offsetMail;
	
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
