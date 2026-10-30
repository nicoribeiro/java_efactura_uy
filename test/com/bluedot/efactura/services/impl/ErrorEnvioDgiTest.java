package com.bluedot.efactura.services.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;

/**
 * El error de un envio fallido a DGI queda en el sobre (errorEnvioDgi) con la causa original, para que el panel no lo
 * muestre como "Sin respuesta".
 */
public class ErrorEnvioDgiTest {

	@Test
	public void incluyeLaCausaOriginal() {
		Exception pkix = new Exception("Problem writing SAAJ model to stream",
				new Exception("PKIX path building failed: unable to find valid certification path to requested target"));
		String texto = RecepcionServiceImpl.textoErrorEnvio(APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, pkix));
		assertTrue(texto, texto.endsWith(": PKIX path building failed: unable to find valid certification path to requested target"));
	}

	@Test
	public void sinCausaSoloElError() {
		String texto = RecepcionServiceImpl.textoErrorEnvio(APIException.raise(APIErrors.ERROR_COMUNICACION_DGI));
		assertTrue(texto, !texto.contains(": "));
	}

	@Test
	public void recortaLosTextosLargos() {
		StringBuilder largo = new StringBuilder();
		for (int i = 0; i < 3000; i++)
			largo.append('x');
		String texto = RecepcionServiceImpl.textoErrorEnvio(
				APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, new Exception("a", new Exception(largo.toString()))));
		assertEquals(2000, texto.length());
	}
}
