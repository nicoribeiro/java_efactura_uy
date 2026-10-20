package com.bluedot.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.Test;

/**
 * El RUT y la serie de un CFE recibido (los elige el emisor) y el Output de reportes y pruebas terminan en rutas de
 * archivos: no pueden salir de la carpeta base.
 */
public class IOTest {

	@Test
	public void segmentoDejaIgualLosDatosNormales() {
		assertEquals("218757590018", IO.segmento("218757590018"));
		assertEquals("A", IO.segmento("A"));
		assertEquals("reporte-2026_10", IO.segmento("reporte-2026_10"));
	}

	@Test
	public void segmentoNoPuedeSubirNiSerAbsoluto() {
		assertEquals("x____219999990016", IO.segmento("x/../219999990016"));
		assertEquals("__", IO.segmento(".."));
		assertEquals("_etc_passwd", IO.segmento("/etc/passwd"));
		assertEquals("_", IO.segmento(null));
		assertEquals("_", IO.segmento(""));
	}

	@Test
	public void dentroDeQuedaDentroDeLaBase() throws IOException {
		File base = Files.createTempDirectory("io-test").toFile();
		File f = IO.dentroDe(base, "218757590018", "2026");
		assertEquals(new File(new File(base, "218757590018"), "2026").getPath(), f.getPath());
		File traversal = IO.dentroDe(base, "x/../../..", "2026");
		assertTrue(traversal.getCanonicalPath().startsWith(base.getCanonicalPath() + File.separator));
	}

	@Test
	public void dentroDeRechazaUnLinkQueSaleDeLaBase() throws IOException {
		File base = Files.createTempDirectory("io-test").toFile();
		File afuera = Files.createTempDirectory("io-afuera").toFile();
		Files.createSymbolicLink(new File(base, "link").toPath(), afuera.toPath());
		try {
			IO.dentroDe(base, "link", "2026");
		} catch (IOException e) {
			return;
		}
		fail("Se acepto una ruta que por un link sale de la base");
	}
}
