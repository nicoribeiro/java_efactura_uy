package com.bluedot.commons.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LimiteIntentosLoginTest {

	private static final long MIN = 60000L;

	private static int bloqueo(LimiteIntentosLogin l, String usuario, String ip, long ahora) {
		return l.minutosBloqueado(usuario, ip, ahora, 5, 20, 15);
	}

	@Test
	public void bloqueaAlQuintoFalloDelUsuario() {
		LimiteIntentosLogin l = new LimiteIntentosLogin();
		for (int i = 0; i < 4; i++)
			l.registrarFalla("a@x.com", "1.1.1." + i, i * MIN);
		assertEquals(0, bloqueo(l, "a@x.com", "9.9.9.9", 4 * MIN));
		l.registrarFalla("A@X.com", "1.1.1.9", 4 * MIN);
		// La primera falla (minuto 0) sale de la ventana en el minuto 15
		assertEquals(11, bloqueo(l, "a@x.com", "9.9.9.9", 4 * MIN));
		assertEquals(0, bloqueo(l, "otro@x.com", "9.9.9.9", 4 * MIN));
	}

	@Test
	public void seDesbloqueaAlPasarLaVentana() {
		LimiteIntentosLogin l = new LimiteIntentosLogin();
		for (int i = 0; i < 5; i++)
			l.registrarFalla("a@x.com", null, 0);
		assertTrue(bloqueo(l, "a@x.com", null, 14 * MIN) > 0);
		assertEquals(0, bloqueo(l, "a@x.com", null, 15 * MIN));
	}

	@Test
	public void loginCorrectoBorraLasFallasDelUsuario() {
		LimiteIntentosLogin l = new LimiteIntentosLogin();
		for (int i = 0; i < 5; i++)
			l.registrarFalla("a@x.com", "1.1.1.1", 0);
		l.registrarExito("a@x.com");
		assertEquals(0, bloqueo(l, "a@x.com", "2.2.2.2", MIN));
	}

	@Test
	public void bloqueaLaIpConMuchosUsuarios() {
		LimiteIntentosLogin l = new LimiteIntentosLogin();
		for (int i = 0; i < 20; i++)
			l.registrarFalla("u" + i + "@x.com", "6.6.6.6", 0);
		assertTrue(bloqueo(l, "nuevo@x.com", "6.6.6.6", MIN) > 0);
		assertEquals(0, bloqueo(l, "nuevo@x.com", "7.7.7.7", MIN));
	}
}
