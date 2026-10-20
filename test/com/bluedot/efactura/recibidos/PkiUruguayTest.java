package com.bluedot.efactura.recibidos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.DatatypeConverter;

import org.junit.Test;
import org.w3c.dom.Element;

import com.bluedot.commons.utils.XML;

/**
 * Cadena de confianza de los certificados de los sobres recibidos. Los certificados de pki/ son de prueba (raiz,
 * intermedia y hoja generadas para el test, mas uno autofirmado con el mismo RUC): ninguno es real.
 */
public class PkiUruguayTest {

	private static X509Certificate pki(String nombre) throws Exception {
		return PkiUruguay.deTexto(new String(Files.readAllBytes(Paths.get("test/com/bluedot/efactura/recibidos/pki", nombre + ".pem")),
				StandardCharsets.US_ASCII));
	}

	@Test
	public void cadenaValida() throws Exception {
		assertNull(PkiUruguay.validar(pki("hoja"), new Date(), pki("raiz"), Collections.singletonList(pki("inter"))));
	}

	@Test
	public void autofirmadoConElMismoRucNoPasa() throws Exception {
		assertNotNull(PkiUruguay.validar(pki("autofirmado"), new Date(), pki("raiz"), Collections.singletonList(pki("inter"))));
	}

	@Test
	public void sinLaIntermediaNoPasa() throws Exception {
		assertNotNull(PkiUruguay.validar(pki("hoja"), new Date(), pki("raiz"), Collections.<X509Certificate> emptyList()));
	}

	@Test
	public void antesDeLaVigenciaNoPasa() throws Exception {
		Date antes = new GregorianCalendar(2000, 0, 1).getTime();
		assertNotNull(PkiUruguay.validar(pki("hoja"), antes, pki("raiz"), Collections.singletonList(pki("inter"))));
	}

	@Test
	public void rucDelCertificado() throws Exception {
		assertEquals("219999990016", ControlesSobre.rucDelCertificado(pki("hoja")));
	}

	/** Los certificados reales de conf/certs: la ACRN publicada por UCE/Agesic, y Abitab y Correo firmados por ella */
	@Test
	public void configuracionReal() throws Exception {
		X509Certificate raiz = PkiUruguay.leer(PkiUruguay.RAIZ);
		assertEquals("5533A0401F612C688EBCE5BF53F2EC14A734EB178BFAE00E50E85DAE6723078A",
				DatatypeConverter.printHexBinary(MessageDigest.getInstance("SHA-256").digest(raiz.getEncoded())));
		Date fecha = new GregorianCalendar(2026, 9, 20).getTime();
		for (String intermedio : PkiUruguay.INTERMEDIOS)
			assertNull(intermedio, PkiUruguay.validar(PkiUruguay.leer(intermedio), fecha, raiz, Collections.<X509Certificate> emptyList()));
	}

	/** Un CFE escondido en la Adenda (xs:anyType) no cuenta: correria la posicion de los siguientes */
	@Test
	public void cfesDelSobreIgnoraUnCfeEnLaAdenda() throws Exception {
		String ns = "http://cfe.dgi.gub.uy";
		String sobre = "<EnvioCFE_entreEmpresas xmlns=\"" + ns + "\"><Caratula/>"
				+ "<CFE_Adenda><CFE version=\"1.0\"><Id>1</Id></CFE><Adenda><CFE version=\"1.0\"><Id>escondido</Id></CFE></Adenda></CFE_Adenda>"
				+ "<CFE_Adenda><CFE version=\"1.0\"><Id>2</Id></CFE></CFE_Adenda></EnvioCFE_entreEmpresas>";
		List<Element> cfes = ControlesRecepcion.cfesDelSobre(XML.loadXMLFromString(sobre));
		assertEquals(2, cfes.size());
		assertEquals(Arrays.asList("1", "2"), Arrays.asList(cfes.get(0).getTextContent(), cfes.get(1).getTextContent()));
	}
}
