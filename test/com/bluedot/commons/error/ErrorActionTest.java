package com.bluedot.commons.error;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Resultados con estado de error que devuelve un controlador directamente (no por APIException) */
public class ErrorActionTest {

	/** 400 es error (antes pasaba como exito: la condicion era 400 < estado) */
	@Test
	public void estadosDeError() {
		assertTrue(ErrorAction.esEstadoDeError(400));
		assertTrue(ErrorAction.esEstadoDeError(404));
		assertTrue(ErrorAction.esEstadoDeError(500));
		assertTrue(ErrorAction.esEstadoDeError(199));
		assertFalse(ErrorAction.esEstadoDeError(200));
		assertFalse(ErrorAction.esEstadoDeError(204));
		assertFalse(ErrorAction.esEstadoDeError(302));
	}

	/** Sin error_cause en el contexto (antes NullPointerException al hacer toString) */
	@Test
	public void causaSinErrorCause() {
		assertEquals("Not Found", ErrorAction.causa(null, 404));
		assertEquals("Error 499", ErrorAction.causa(null, 499));
		assertEquals("La sesion vencio", ErrorAction.causa("La sesion vencio", 401));
	}
}
