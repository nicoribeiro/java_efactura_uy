package com.bluedot.efactura.recibidos;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Cadena de confianza de los certificados con los que firman los emisores (PKI Uruguay): el certificado tiene que estar
 * emitido por una autoridad certificadora acreditada (Abitab, Correo Uruguayo) firmada por la Autoridad Certificadora Raiz
 * Nacional (ACRN), y todos vigentes en la fecha de la firma. Sin esto cualquiera podia mandar un sobre firmado con un
 * certificado propio (autofirmado) y el RUC de otra empresa. No se consultan listas de revocacion (CRL).
 */
public final class PkiUruguay {

	static final String RAIZ = "/certs/acrn-autoridad-certificadora-raiz-nacional.pem";
	static final List<String> INTERMEDIOS = Arrays.asList("/certs/abitab-id-digital.pem", "/certs/correo-uruguayo-ca.pem");

	private static X509Certificate raiz;
	private static List<X509Certificate> intermedios;

	private PkiUruguay() {
	}

	/**
	 * @return null si el certificado encadena con la ACRN y todo estaba vigente en la fecha; si no, el motivo
	 */
	public static String validar(X509Certificate cert, Date fecha) {
		try {
			cargar();
		} catch (Exception e) {
			// Sin los certificados de la PKI no se puede controlar: no es culpa del emisor
			return null;
		}
		return validar(cert, fecha, raiz, intermedios);
	}

	static String validar(X509Certificate cert, Date fecha, X509Certificate raiz, List<X509Certificate> intermedios) {
		if (cert == null)
			return "no hay certificado";
		try {
			X509CertSelector selector = new X509CertSelector();
			selector.setCertificate(cert);
			PKIXBuilderParameters parametros = new PKIXBuilderParameters(Collections.singleton(new TrustAnchor(raiz, null)), selector);
			List<Certificate> certificados = new ArrayList<Certificate>(intermedios);
			certificados.add(cert);
			parametros.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(certificados)));
			parametros.setRevocationEnabled(false);
			parametros.setDate(fecha != null ? fecha : new Date());
			CertPathBuilder.getInstance("PKIX").build(parametros);
			return null;
		} catch (CertPathBuilderException e) {
			return "el certificado (emitido por " + cert.getIssuerX500Principal().getName()
					+ ") no es de una autoridad certificadora acreditada de Uruguay o no estaba vigente en la fecha de la firma";
		} catch (Exception e) {
			return "no se pudo validar la cadena del certificado: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
		}
	}

	private static synchronized void cargar() throws Exception {
		if (raiz != null)
			return;
		X509Certificate r = leer(RAIZ);
		List<X509Certificate> lista = new ArrayList<X509Certificate>();
		for (String recurso : INTERMEDIOS)
			lista.add(leer(recurso));
		intermedios = lista;
		raiz = r;
	}

	/** Certificado PEM del classpath (puede tener comentarios antes de "-----BEGIN") */
	static X509Certificate leer(String recurso) throws Exception {
		try (InputStream in = PkiUruguay.class.getResourceAsStream(recurso)) {
			if (in == null)
				throw new FileNotFoundException(recurso);
			return deTexto(new Scanner(in, StandardCharsets.US_ASCII.name()).useDelimiter("\\A").next());
		}
	}

	static X509Certificate deTexto(String pem) throws Exception {
		int inicio = pem.indexOf("-----BEGIN");
		return (X509Certificate) CertificateFactory.getInstance("X.509")
				.generateCertificate(new ByteArrayInputStream(pem.substring(Math.max(inicio, 0)).getBytes(StandardCharsets.US_ASCII)));
	}
}
