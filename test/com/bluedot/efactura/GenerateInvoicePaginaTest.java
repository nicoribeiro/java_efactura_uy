package com.bluedot.efactura;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Corte de pagina del detalle del PDF: un item que ocupa varias lineas hacia saltar la fila de 15 a 17, la condicion
 * row == 16 no se cumplia y los items seguian dibujandose sobre los totales.
 */
public class GenerateInvoicePaginaTest {

	@Test
	public void lineasDeUnaDescripcion() {
		assertEquals(1, GenerateInvoice.lineasDe("", 64));
		assertEquals(1, GenerateInvoice.lineasDe("Producto corto", 64));
		// 3 palabras de 40 letras: no entran dos por linea de 64
		String palabra = "abcdefghijabcdefghijabcdefghijabcdefghij";
		assertEquals(3, GenerateInvoice.lineasDe(palabra + " " + palabra + " " + palabra, 64));
	}

	@Test
	public void saltaSiElItemNoEntraEnLoQueQueda() {
		// 15 filas por pagina: en la fila 15 entra un item de 1 linea, no uno de 3
		assertFalse(GenerateInvoice.saltarPagina(15, 1, 15));
		assertTrue(GenerateInvoice.saltarPagina(15, 3, 15));
		assertTrue(GenerateInvoice.saltarPagina(16, 1, 15));
		assertFalse(GenerateInvoice.saltarPagina(13, 3, 15));
	}

	@Test
	public void conLaPaginaVaciaNoSalta() {
		// un item mas largo que la pagina se dibuja igual en una pagina nueva (no hay donde mas)
		assertFalse(GenerateInvoice.saltarPagina(1, 20, 15));
	}
}
