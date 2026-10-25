package com.bluedot.efactura.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * El puerto del correo se valida con trim pero se usaba sin trim (Integer.parseInt(" 25") tira NumberFormatException): el
 * aviso de vencimiento y el mail de los CFE a los clientes no salian.
 */
public class EmpresaPuertoTest {

	@Test
	public void puertoSinEspacios() {
		Empresa empresa = new Empresa();
		empresa.setPuertoRecepcion(" 25 ");
		assertEquals("25", empresa.getPuertoRecepcion());
		assertEquals(25, Integer.parseInt(empresa.getPuertoRecepcion()));
	}

	@Test
	public void puertoNull() {
		assertNull(new Empresa().getPuertoRecepcion());
	}
}
