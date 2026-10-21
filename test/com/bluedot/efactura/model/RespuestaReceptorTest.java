package com.bluedot.efactura.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/** Estado de cada CFE en la respuesta ME_ del receptor (datos ficticios) */
public class RespuestaReceptorTest {

	private static String det(String tipo, String serie, String nro, String estado) {
		return "<ACKCFE_Det><TipoCFE>" + tipo + "</TipoCFE><Serie>" + serie + "</Serie><NroCFE>" + nro + "</NroCFE>"
				+ (estado != null ? "<Estado>" + estado + "</Estado>" : "") + "</ACKCFE_Det>";
	}

	private static String ackCfe(String... dets) {
		return "<ACKCFE xmlns=\"http://cfe.dgi.gub.uy\" version=\"1.0\">" + String.join("", dets) + "</ACKCFE>";
	}

	@Test
	public void estadoPorCfe() {
		RespuestaReceptor r = RespuestaReceptor.leer(ackCfe(det("111", "A", "1", "AE"), det("111", "A", "2", "BE")));
		assertEquals("AE", r.estado(TipoDoc.eFactura, "A", 1));
		assertEquals("BE", r.estado(TipoDoc.eFactura, "A", 2));
		// Otro CFE del mismo sobre que no esta en la respuesta
		assertEquals(RespuestaReceptor.RECIBIDA, r.estado(TipoDoc.eFactura, "A", 3));
		assertEquals(RespuestaReceptor.RECIBIDA, r.estado(TipoDoc.eFactura, "B", 1));
	}

	@Test
	public void numerosConCerosYSinEstado() {
		RespuestaReceptor r = RespuestaReceptor.leer(ackCfe(det("0111", "A", "0002295", "BE"), det("111", "A", "7", null)));
		assertEquals("BE", r.estado(TipoDoc.eFactura, "A", 2295));
		assertEquals(RespuestaReceptor.RECIBIDA, r.estado(TipoDoc.eFactura, "A", 7));
	}

	@Test
	public void payloadIlegible() {
		assertEquals(RespuestaReceptor.RECIBIDA, RespuestaReceptor.leer("no es xml").estado(TipoDoc.eFactura, "A", 1));
		assertEquals(RespuestaReceptor.RECIBIDA, RespuestaReceptor.leer(null).estado(TipoDoc.eFactura, "A", 1));
	}
}
