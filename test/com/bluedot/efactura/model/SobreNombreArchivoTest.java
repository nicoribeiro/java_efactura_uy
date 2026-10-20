package com.bluedot.efactura.model;

import static org.junit.Assert.assertEquals;

import java.util.GregorianCalendar;

import org.junit.Test;

/**
 * El nombre del archivo del sobre lleva la fecha de la caratula (DGI), no la del dia en que se manda el mail: si no
 * coinciden el receptor rechaza el sobre con S01.
 */
public class SobreNombreArchivoTest {

	private static final String RUT = "123456789012";

	@Test
	public void usaLaFechaDeLaCaratula() {
		GregorianCalendar caratula = new GregorianCalendar(2026, 0, 15, 20, 30, 0);
		assertEquals("Sob_123456789012_20260115_1234.xml", Sobre.nombreArchivo(RUT, caratula.getTime(), 1234));
	}

	@Test
	public void cercaDeLaMedianoche() {
		assertEquals("Sob_123456789012_20261231_1.xml", Sobre.nombreArchivo(RUT, new GregorianCalendar(2026, 11, 31, 23, 59, 59).getTime(), 1));
		assertEquals("Sob_123456789012_20270101_2.xml", Sobre.nombreArchivo(RUT, new GregorianCalendar(2027, 0, 1, 0, 0, 1).getTime(), 2));
	}
}
