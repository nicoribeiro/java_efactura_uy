package com.bluedot.efactura.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.bluedot.commons.utils.PublicPrivateKey;
import com.play4jpa.jpa.models.Model;

@Entity
public class FirmaDigital extends Model<FirmaDigital>{

	/**
	 * 
	 */
	private static final long serialVersionUID = 6134216142856632690L;
	
	public static String KEY_ALIAS = "keyAlias";
	public static String KEYSTORE_PASSWORD = "password";
	
	@Id
	@GeneratedValue
	private int id;
	
	@OneToOne
	private Empresa empresa;
	
	@Column(columnDefinition="TEXT")
	private String certificate;
	
	@Column(columnDefinition="TEXT")
	private String privateKey;
	
	private Date validaDesde;
	
	private Date validaHasta;

	public FirmaDigital() {
		super();
	}
	
	public Empresa getEmpresa() {
		return empresa;
	}

	public void setEmpresa(Empresa empresa) {
		this.empresa = empresa;
	}

	public String getCertificate() {
		return certificate;
	}

	public void setCertificate(String certificate) {
		this.certificate = certificate;
	}

	public String getPrivateKey() {
		return privateKey;
	}

	public void setPrivateKey(String privateKey) {
		this.privateKey = privateKey;
	}

	public int getId() {
		return id;
	}
	
	public KeyStore getKeyStore(){
		try {
			KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			keyStore.load(null, KEYSTORE_PASSWORD.toCharArray());
			
			CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
			
			InputStream in = new ByteArrayInputStream(certificate.getBytes());
			
			java.security.cert.Certificate[] chain = {};
			chain = certificateFactory.generateCertificates(in).toArray(chain);
			
			PrivateKey privKey = PublicPrivateKey.pemFileLoadPrivateKeyPkcs1OrPkcs8Encoded(privateKey); 
			
			keyStore.setEntry(KEY_ALIAS,
					new KeyStore.PrivateKeyEntry(privKey, chain),
					new KeyStore.PasswordProtection(KEYSTORE_PASSWORD.toCharArray())
					);
			
			return keyStore;
			
		} catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException | InvalidKeySpecException e) {
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	public void setId(int id) {
		this.id = id;
	}

	public Date getValidaDesde() {
		return validaDesde;
	}

	public void setValidaDesde(Date validaDesde) {
		this.validaDesde = validaDesde;
	}

	public Date getValidaHasta() {
		return validaHasta;
	}

	public void setValidaHasta(Date validaHasta) {
		this.validaHasta = validaHasta;
	}
}
