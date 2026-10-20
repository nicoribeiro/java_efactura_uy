package com.bluedot.commons.utils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.util.Scanner;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

/**
 * Conexiones HTTPS a servidores que mandan mal la cadena de certificados (ej. INE: manda un intermedio que no es el que
 * firmo su certificado, y Java 8 no lo baja solo como los navegadores). En lugar de no verificar nada (lo que habia antes,
 * y para toda la JVM) se confia en los certificados de Java mas los indicados, y solo en esa conexion.
 */
public final class TlsExtra {

	private TlsExtra() {
	}

	/**
	 * SSLSocketFactory que confia en los certificados de Java (cacerts) y ademas en los PEM indicados (recursos del
	 * classpath, ej. "/certs/x.pem"). No cambia el default de la JVM: se usa con HttpsURLConnection.setSSLSocketFactory.
	 */
	public static SSLSocketFactory conCertificados(String... recursos) throws GeneralSecurityException, IOException {
		KeyStore almacen = KeyStore.getInstance(KeyStore.getDefaultType());
		File cacerts = new File(System.getProperty("java.home"), "lib" + File.separator + "security" + File.separator + "cacerts");
		try (InputStream in = new FileInputStream(cacerts)) {
			almacen.load(in, null);
		}

		CertificateFactory fabrica = CertificateFactory.getInstance("X.509");
		for (String recurso : recursos) {
			try (InputStream in = TlsExtra.class.getResourceAsStream(recurso)) {
				if (in == null)
					throw new FileNotFoundException(recurso);
				almacen.setCertificateEntry("extra:" + recurso, fabrica.generateCertificate(new ByteArrayInputStream(soloPem(in))));
			}
		}

		TrustManagerFactory confianza = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		confianza.init(almacen);
		SSLContext contexto = SSLContext.getInstance("TLS");
		contexto.init(null, confianza.getTrustManagers(), null);
		return contexto.getSocketFactory();
	}

	/** El archivo puede tener comentarios antes del certificado: se toma desde "-----BEGIN" */
	private static byte[] soloPem(InputStream in) {
		try (Scanner s = new Scanner(in, StandardCharsets.US_ASCII.name()).useDelimiter("\\A")) {
			String texto = s.hasNext() ? s.next() : "";
			int inicio = texto.indexOf("-----BEGIN");
			return (inicio >= 0 ? texto.substring(inicio) : texto).getBytes(StandardCharsets.US_ASCII);
		}
	}
}
