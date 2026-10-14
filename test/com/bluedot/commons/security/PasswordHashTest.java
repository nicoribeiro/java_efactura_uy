package com.bluedot.commons.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordHashTest {

	@Test
	public void verificaLaMismaContrasena() {
		String hash = PasswordHash.hash("contraseña 1");
		assertTrue(PasswordHash.verificar("contraseña 1", hash));
		assertFalse(PasswordHash.verificar("contraseña 2", hash));
		assertFalse(PasswordHash.verificar("", hash));
		assertFalse(PasswordHash.verificar(null, hash));
	}

	@Test
	public void cadaHashTieneSuSal() {
		assertNotEquals(PasswordHash.hash("igual"), PasswordHash.hash("igual"));
	}

	@Test
	public void formatoConIteraciones() {
		String hash = PasswordHash.hash("x");
		String[] partes = hash.split("\\$");
		assertEquals(4, partes.length);
		assertEquals("pbkdf2-sha256", partes[0]);
		assertEquals(String.valueOf(PasswordHash.ITERACIONES), partes[1]);
		assertTrue(hash.length() <= 128);
	}

	@Test
	public void respetaLasIteracionesGuardadas() {
		String hash = PasswordHash.hash("vieja", 1000);
		assertTrue(PasswordHash.verificar("vieja", hash));
	}

	@Test
	public void hashMalFormadoNoVerifica() {
		assertFalse(PasswordHash.verificar("x", null));
		assertFalse(PasswordHash.verificar("x", "basura"));
		assertFalse(PasswordHash.verificar("x", "pbkdf2-sha256$abc$$"));
		assertFalse(PasswordHash.verificar("x", "md5$1$AA==$AA=="));
	}

	@Test
	public void shaViejo() {
		assertTrue(PasswordHash.igualesSha(User.getSha512("a"), User.getSha512("a")));
		assertFalse(PasswordHash.igualesSha(User.getSha512("a"), User.getSha512("b")));
		assertFalse(PasswordHash.igualesSha(User.getSha512("a"), null));
	}
}
