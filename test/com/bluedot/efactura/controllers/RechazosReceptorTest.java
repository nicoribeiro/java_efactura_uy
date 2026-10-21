package com.bluedot.efactura.controllers;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.bluedot.efactura.model.Empresa;

/**
 * Contadores de rechazos de las empresas receptoras (inicio del panel) a partir de respuestas M_ y ME_ de prueba (datos
 * ficticios).
 */
public class RechazosReceptorTest {

	private static final Empresa RECEPTOR_A = new Empresa("100000000017", "Receptor A", null);
	private static final Empresa RECEPTOR_B = new Empresa("200000000014", "Receptor B", null);

	private static String ackSobre(String estado, String motivo, String glosa) {
		return "<ACKSobre xmlns=\"http://cfe.dgi.gub.uy\" version=\"1.0\"><Caratula><RUCReceptor>100000000017</RUCReceptor>"
				+ "<RUCEmisor>300000000011</RUCEmisor><IDRespuesta>1</IDRespuesta><NomArch>Sob_300000000011_20260115_10.xml</NomArch>"
				+ "<FecHRecibido>2026-01-15T10:00:00-03:00</FecHRecibido><IDEmisor>10</IDEmisor><IDReceptor>20</IDReceptor>"
				+ "<CantidadCFE>1</CantidadCFE><Tmst>2026-01-15T10:00:01-03:00</Tmst></Caratula><Detalle><Estado>" + estado + "</Estado>"
				+ (motivo != null ? "<MotivosRechazo><Motivo>" + motivo + "</Motivo><Glosa>" + glosa + "</Glosa></MotivosRechazo>" : "")
				+ "</Detalle></ACKSobre>";
	}

	private static String det(int ordinal, String estado, long nro, String motivo) {
		return "<ACKCFE_Det><Nro_ordinal>" + ordinal + "</Nro_ordinal><TipoCFE>111</TipoCFE><Serie>A</Serie><NroCFE>" + nro + "</NroCFE>"
				+ "<FechaCFE>2026-01-15</FechaCFE><TmstCFE>2026-01-15T10:00:00-03:00</TmstCFE><Estado>" + estado + "</Estado>"
				+ (motivo != null ? "<MotivosRechazoCF><Motivo>" + motivo + "</Motivo><Glosa>Glosa " + motivo + "</Glosa></MotivosRechazoCF>" : "")
				+ "</ACKCFE_Det>";
	}

	private static String ackCfe(String... dets) {
		return "<ACKCFE xmlns=\"http://cfe.dgi.gub.uy\" version=\"1.0\"><Caratula><RUCReceptor>100000000017</RUCReceptor>"
				+ "<RUCEmisor>300000000011</RUCEmisor><IDRespuesta>2</IDRespuesta><NomArch>Sob_300000000011_20260115_10.xml</NomArch>"
				+ "<FecHRecibido>2026-01-15T10:00:00-03:00</FecHRecibido><IDEmisor>10</IDEmisor><IDReceptor>20</IDReceptor>"
				+ "<CantenSobre>" + dets.length + "</CantenSobre><CantResponden>" + dets.length + "</CantResponden>"
				+ "<Tmst>2026-01-15T10:00:01-03:00</Tmst></Caratula>" + String.join("", dets) + "</ACKCFE>";
	}

	@Test
	public void sobresRechazadosPorEmpresaYMotivo() {
		SobresController.Rechazos r = new SobresController.Rechazos();
		r.agregarSobre(RECEPTOR_A, ackSobre("BS", "S01", "Formato del archivo no es el indicado"), 10);
		r.agregarSobre(RECEPTOR_A, ackSobre("BS", "S01", "Formato del archivo no es el indicado"), 11);
		r.agregarSobre(RECEPTOR_B, ackSobre("BS", "S03", "Certificado vencido"), 12);
		// Sin respuesta legible: cuenta igual (el estado BS esta en la base), sin motivo
		r.agregarSobre(RECEPTOR_B, null, 13);

		JSONObject json = r.json();
		assertEquals(4, json.getInt("total"));
		JSONArray empresas = json.getJSONArray("porEmpresa");
		assertEquals("100000000017", empresas.getJSONObject(0).getString("rut"));
		assertEquals(2, empresas.getJSONObject(0).getInt("cantidad"));
		JSONObject motivo = empresas.getJSONObject(0).getJSONArray("motivos").getJSONObject(0);
		assertEquals("S01", motivo.getString("motivo"));
		assertEquals(2, motivo.getInt("cantidad"));
		assertEquals("Formato del archivo no es el indicado", motivo.getString("glosa"));
		assertEquals(2, empresas.getJSONObject(1).getInt("cantidad"));
		assertEquals(1, empresas.getJSONObject(1).getJSONArray("motivos").length());
	}

	@Test
	public void cfesRechazadosCuentaSoloLosBE() {
		SobresController.Rechazos r = new SobresController.Rechazos();
		r.agregarCfes(RECEPTOR_A, ackCfe(det(1, "AE", 1, null), det(2, "BE", 2, "E05"), det(3, "BE", 3, "E05")), 10);
		r.agregarCfes(RECEPTOR_B, ackCfe(det(1, "AE", 4, null)), 11);
		r.agregarCfes(RECEPTOR_B, "no es xml", 12);

		JSONObject json = r.json();
		assertEquals(2, json.getInt("total"));
		JSONArray empresas = json.getJSONArray("porEmpresa");
		assertEquals(1, empresas.length());
		JSONObject a = empresas.getJSONObject(0);
		assertEquals(2, a.getInt("cantidad"));
		assertEquals("E05", a.getJSONArray("motivos").getJSONObject(0).getString("motivo"));
		JSONObject cfe = a.getJSONArray("listados").getJSONObject(0);
		assertEquals(111, cfe.getInt("idTipoDoc"));
		assertEquals("A", cfe.getString("serie"));
		assertEquals(2, cfe.getLong("nro"));
	}
}
