package com.bluedot.efactura;

import static org.junit.Assert.assertEquals;

import org.json.JSONArray;
import org.junit.Test;

/**
 * "ADENDA - DATOS INTERNOS" no muestra Entrega ni Notas (tienen su propia seccion). Al sacarlos recorriendo hacia adelante
 * se salteaba el elemento siguiente: con Entrega y Notas seguidos, Notas salia dos veces en el PDF.
 */
public class GenerateInvoiceAdendaTest {

	@Test
	public void sacaEntregaYNotasSeguidos() {
		JSONArray adenda = new JSONArray("[{\"Entrega\":[{\"Direccion\":\"x\"}]},{\"Notas\":\"nota\"},{\"OC\":\"1\"}]");
		JSONArray datosInternos = GenerateInvoice.datosInternos(adenda);
		assertEquals(1, datosInternos.length());
		assertEquals("1", datosInternos.getJSONObject(0).getString("OC"));
	}

	@Test
	public void sacaDosNotasSeguidas() {
		JSONArray adenda = new JSONArray("[{\"Notas\":\"a\"},{\"Notas\":\"b\"},{\"OC\":\"1\"},{\"Otro\":\"2\"}]");
		assertEquals(2, GenerateInvoice.datosInternos(adenda).length());
	}

	@Test
	public void noModificaLaAdendaOriginal() {
		JSONArray adenda = new JSONArray("[{\"Notas\":\"a\"},{\"OC\":\"1\"}]");
		GenerateInvoice.datosInternos(adenda);
		assertEquals(2, adenda.length());
	}
}
