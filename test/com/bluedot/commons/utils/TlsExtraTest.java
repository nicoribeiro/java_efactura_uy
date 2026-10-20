package com.bluedot.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

import org.junit.Test;

/**
 * El servidor de INE manda mal la cadena de certificados. Antes se desactivaba la verificacion para toda la JVM; ahora se
 * agrega el intermedio que falta solo para esa conexion.
 */
public class TlsExtraTest {

	private static final String CERTIFICADO_INE = "/certs/sectigo-public-server-authentication-ca-dv-r36.pem";
	private static final String URL_INE = "https://www5.ine.gub.uy/";

	@Test
	public void cargaElIntermedioDeIne() throws Exception {
		assertNotNull(TlsExtra.conCertificados(CERTIFICADO_INE));
	}

	@Test(expected = java.io.FileNotFoundException.class)
	public void faltaElCertificado() throws Exception {
		TlsExtra.conCertificados("/certs/no-existe.pem");
	}

	/** Contra el servidor real de INE (red): solo con INE_TLS=1 */
	@Test
	public void ineSoloValidaConElIntermedio() throws Exception {
		assumeTrue("Sin INE_TLS=1: no se prueba contra el servidor de INE", "1".equals(System.getenv("INE_TLS")));

		try {
			conectar(null);
			fail("Con los certificados de Java solos ya valida: el intermedio extra puede no hacer falta");
		} catch (SSLHandshakeException e) {
			// esperado: el servidor manda mal la cadena
		}
		assertEquals(200, conectar(TlsExtra.conCertificados(CERTIFICADO_INE)));
	}

	private static int conectar(SSLSocketFactory fabrica) throws Exception {
		HttpsURLConnection c = (HttpsURLConnection) new URL(URL_INE).openConnection();
		if (fabrica != null)
			c.setSSLSocketFactory(fabrica);
		c.setRequestMethod("HEAD");
		c.setConnectTimeout(15000);
		c.setReadTimeout(15000);
		return c.getResponseCode();
	}
}
