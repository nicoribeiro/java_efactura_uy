package com.bluedot.efactura.model;

import static org.junit.Assert.assertEquals;

import java.util.GregorianCalendar;

import org.junit.Test;

/**
 * El nombre del archivo del sobre lleva la fecha de la caratula (DGI). Un sobre armado el 7/10 y mandado por mail el 8/10
 * salio como Sob_..._20261008_... y el receptor lo rechazo con S01 ("No coincide el nombre del documento ... con el
 * definido por DGI [Sob_..._20261007_...]").
 */
public class SobreNombreArchivoTest {

	@Test
	public void usaLaFechaDeLaCaratulaNoLaDeHoy() {
		GregorianCalendar caratula = new GregorianCalendar(2026, 9, 7, 20, 56, 25);
		assertEquals("Sob_219999990016_20261007_312218.xml", Sobre.nombreArchivo("219999990016", caratula.getTime(), 312218));
	}

	@Test
	public void cercaDeLaMedianoche() {
		assertEquals("Sob_219999990016_20261231_1.xml",
				Sobre.nombreArchivo("219999990016", new GregorianCalendar(2026, 11, 31, 23, 59, 59).getTime(), 1));
		assertEquals("Sob_219999990016_20270101_2.xml",
				Sobre.nombreArchivo("219999990016", new GregorianCalendar(2027, 0, 1, 0, 0, 1).getTime(), 2));
	}
}
