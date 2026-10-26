package com.bluedot.commons.utils;

import static org.junit.Assert.assertEquals;

import org.json.JSONObject;
import org.junit.Test;

/** merge con objetos vacios: getNames de {} devuelve null (antes NullPointerException) */
public class JSONUtilsTest {

	@Test
	public void segundoVacio() {
		JSONObject r = JSONUtils.merge(new JSONObject("{\"a\":1}"), new JSONObject());
		assertEquals(1, r.length());
		assertEquals(1, r.getInt("a"));
	}

	@Test
	public void primeroNullYSegundoVacio() {
		assertEquals(0, JSONUtils.merge(null, new JSONObject()).length());
	}

	@Test
	public void primeroVacioYSegundoNull() {
		assertEquals(0, JSONUtils.merge(new JSONObject(), null).length());
	}

	@Test
	public void combinaYElSegundoPisa() {
		JSONObject r = JSONUtils.merge(new JSONObject("{\"a\":1,\"b\":1}"), new JSONObject("{\"b\":2,\"c\":3}"));
		assertEquals(3, r.length());
		assertEquals(1, r.getInt("a"));
		assertEquals(2, r.getInt("b"));
		assertEquals(3, r.getInt("c"));
	}
}
