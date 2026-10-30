package com.bluedot.efactura.pool;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.junit.Test;

import dgi.soap.recepcion.WSEFacturaSoapPort;

/**
 * Desde 2026-10-29 DGI usa un certificado bajo Certum Trusted Root CA, que no esta en los cacerts de Java 8: sin la raiz
 * todos los envios fallaban con PKIX.
 */
public class DgiTlsTest {

	/** Puertos de los servicios de DGI (recepcion, consultas y RUT, de prueba y produccion) */
	private static final int[] PUERTOS_DGI = { 443, 6443, 6440, 6460, 6470, 6475 };

	@Test
	public void elClienteCxfUsaLaRaizDeDgi() {
		JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
		factory.setAddress("https://efactura.dgi.gub.uy:6443/ePrueba/ws_eprueba");
		WSEFacturaSoapPort port = factory.create(WSEFacturaSoapPort.class);

		DgiTls.configurar(ClientProxy.getClient(port));

		HTTPConduit conduit = (HTTPConduit) ClientProxy.getClient(port).getConduit();
		assertSame(DgiTls.fabrica(), conduit.getTlsClientParameters().getSSLSocketFactory());
		// el nombre del servidor se sigue verificando
		assertFalse(conduit.getTlsClientParameters().isDisableCNCheck());
	}

	/** Contra los servidores reales de DGI (red): solo con DGI_TLS=1 */
	@Test
	public void dgiSoloValidaConLaRaizDeCertum() throws Exception {
		assumeTrue("Sin DGI_TLS=1: no se prueba contra DGI", "1".equals(System.getenv("DGI_TLS")));

		for (int puerto : PUERTOS_DGI) {
			try {
				conectar(puerto, null);
				fail("Puerto " + puerto + ": con los certificados de Java solos ya valida, la raiz extra puede no hacer falta");
			} catch (SSLHandshakeException e) {
				// esperado: Java 8 no tiene la raiz de Certum
			}
			assertTrue("Puerto " + puerto, conectar(puerto, DgiTls.fabrica()) > 0);
		}
	}

	private static int conectar(int puerto, SSLSocketFactory fabrica) throws Exception {
		HttpsURLConnection c = (HttpsURLConnection) new URL("https://efactura.dgi.gub.uy:" + puerto + "/").openConnection();
		if (fabrica != null)
			c.setSSLSocketFactory(fabrica);
		c.setConnectTimeout(15000);
		c.setReadTimeout(15000);
		return c.getResponseCode();
	}
}
