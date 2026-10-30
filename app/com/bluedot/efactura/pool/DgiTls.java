package com.bluedot.efactura.pool;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLSocketFactory;

import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.transport.http.HTTPConduit;

import com.bluedot.commons.utils.TlsExtra;

/**
 * TLS de las conexiones a los servicios de DGI: los certificados de Java (cacerts) mas la raiz de Certum, que no esta en
 * Java 8. Se configura en cada cliente CXF, no cambia el default de la JVM.
 */
public class DgiTls {

	static final String CERTIFICADO_DGI = "/certs/certum-trusted-root-ca.pem";

	private static SSLSocketFactory fabrica;

	private DgiTls() {
	}

	static synchronized SSLSocketFactory fabrica() {
		if (fabrica == null) {
			try {
				fabrica = TlsExtra.conCertificados(CERTIFICADO_DGI);
			} catch (GeneralSecurityException | IOException e) {
				throw new IllegalStateException("No se pudo cargar " + CERTIFICADO_DGI, e);
			}
		}
		return fabrica;
	}

	public static void configurar(Client cliente) {
		TLSClientParameters tls = new TLSClientParameters();
		tls.setSSLSocketFactory(fabrica());
		((HTTPConduit) cliente.getConduit()).setTlsClientParameters(tls);
	}
}
