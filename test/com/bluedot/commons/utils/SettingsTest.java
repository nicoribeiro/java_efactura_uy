package com.bluedot.commons.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class SettingsTest {

	private static Settings con(String... claveValor) {
		Properties p = new Properties();
		for (int i = 0; i < claveValor.length; i += 2)
			p.setProperty(claveValor[i], claveValor[i + 1]);
		return new Settings(p);
	}

	/** Sin la clave se devuelve el default (antes siempre false: parseBoolean(null) no tira excepcion) */
	@Test
	public void booleanSinClaveDevuelveElDefault() {
		assertTrue(con().getBooleanProperty("x", true));
		assertFalse(con().getBooleanProperty("x", false));
	}

	@Test
	public void booleanConValor() {
		assertTrue(con("x", "true").getBooleanProperty("x", false));
		assertFalse(con("x", "false").getBooleanProperty("x", true));
	}
}
