package com.bluedot.commons.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CodigoEmailTest {

	@Test
	public void coincideSoloConElMismoCodigo() {
		String hash = CodigoEmail.hash("123456");
		assertTrue(CodigoEmail.coincide(hash, "123456"));
		assertFalse(CodigoEmail.coincide(hash, "123457"));
		assertFalse(CodigoEmail.coincide(hash, ""));
	}

	@Test
	public void cadaHashTieneSuSal() {
		assertNotEquals(CodigoEmail.hash("000000"), CodigoEmail.hash("000000"));
	}

	@Test
	public void hashMalFormadoNoCoincide() {
		assertFalse(CodigoEmail.coincide(null, "123456"));
		assertFalse(CodigoEmail.coincide("basura", "123456"));
	}

	@Test
	public void noGuardaElCodigoEnClaro() {
		assertFalse(CodigoEmail.hash("987654").contains("987654"));
	}
}
