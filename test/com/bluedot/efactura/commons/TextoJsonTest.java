package com.bluedot.efactura.commons;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

/**
 * org.json parsea los decimales como BigDecimal (conserva la escala): safeGetString tiene que seguir devolviendo el
 * texto de antes (el del Double), que es el que va al XML del CFE.
 */
public class TextoJsonTest {

	@Test
	public void decimalesComoAntes() throws Throwable {
		JSONObject json = new JSONObject("{\"a\":12.50,\"b\":1.0,\"c\":1e3,\"d\":12,\"e\":\"12.50\",\"f\":0.000001}");
		assertEquals("12.5", Commons.safeGetString(json, "a"));
		assertEquals("1.0", Commons.safeGetString(json, "b"));
		assertEquals("1000.0", Commons.safeGetString(json, "c"));
		assertEquals("12", Commons.safeGetString(json, "d"));
		assertEquals("12.50", Commons.safeGetString(json, "e"));
		assertEquals("1.0E-6", Commons.safeGetString(json, "f"));
	}
}
