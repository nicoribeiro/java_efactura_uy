package com.bluedot.efactura.commons;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FormatosDgiTest {

	@Test
	public void ruc() {
		assertTrue(FormatosDgi.esRuc("218757590018"));
		assertFalse(FormatosDgi.esRuc(null));
		assertFalse(FormatosDgi.esRuc(""));
		assertFalse(FormatosDgi.esRuc("21875759001"));
		assertFalse(FormatosDgi.esRuc("2187575900180"));
		assertFalse(FormatosDgi.esRuc("x/../21875759"));
		assertFalse(FormatosDgi.esRuc("218757590018\n"));
	}
}
