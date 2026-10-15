package com.bluedot.efactura.commons;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.bluedot.commons.error.APIException;

public class OrdenTest {

	private static final List<String> CAMPOS = Arrays.asList("fecha", "total");

	/**
	 * Tiene que rechazarse. Sin aplicacion de Play (test unitario) el APIException no puede armar su mensaje y sale otra
	 * excepcion: vale cualquiera menos que pase.
	 */
	private static void rechaza(String campo, String sentido) {
		try {
			Orden.leer(campo, sentido, CAMPOS);
		} catch (Throwable e) {
			return;
		}
		fail("Se acepto orden=" + campo + " sentido=" + sentido);
	}

	@Test
	public void sinOrden() throws APIException {
		assertNull(Orden.leer(null, null, CAMPOS));
		assertNull(Orden.leer(" ", "desc", CAMPOS));
	}

	@Test
	public void ascendentePorDefecto() throws APIException {
		Orden orden = Orden.leer("fecha", null, CAMPOS);
		assertEquals("fecha", orden.campo);
		assertTrue(orden.ascendente);
	}

	@Test
	public void descendente() throws APIException {
		assertFalse(Orden.leer("total", "DESC", CAMPOS).ascendente);
		assertTrue(Orden.leer("total", "asc", CAMPOS).ascendente);
	}

	@Test
	public void campoNoPermitido() {
		rechaza("sha_password", "asc");
	}

	@Test
	public void sentidoInvalido() {
		rechaza("fecha", "arriba");
	}
}
