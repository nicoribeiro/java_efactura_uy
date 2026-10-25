package com.bluedot.efactura.microControllers.implementation;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.bluedot.efactura.model.CAE;

/** Porcentaje de uso de los CAE de un tipo: suma todos los CAE, no solo el ultimo */
public class CAEPorcentajeUsoTest {

	private static CAE cae(long inicial, long fin, long siguiente) {
		CAE cae = new CAE();
		cae.setInicial(inicial);
		cae.setFin(fin);
		cae.setSiguiente(siguiente);
		return cae;
	}

	@Test
	public void sumaTodosLosCae() {
		// primero usado entero (siguiente = fin + 1), segundo sin usar
		assertEquals(50.0, CAEMicroControllerDefault.porcentajeUso(Arrays.asList(cae(1, 100, 101), cae(1, 100, 1))), 0.0);
	}

	@Test
	public void unSoloCae() {
		assertEquals(25.0, CAEMicroControllerDefault.porcentajeUso(Collections.singletonList(cae(1, 100, 26))), 0.0);
	}

	@Test
	public void sinCae() {
		assertEquals(0.0, CAEMicroControllerDefault.porcentajeUso(Collections.<CAE> emptyList()), 0.0);
		assertEquals(0.0, CAEMicroControllerDefault.porcentajeUso(null), 0.0);
	}
}
