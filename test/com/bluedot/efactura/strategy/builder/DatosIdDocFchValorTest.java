package com.bluedot.efactura.strategy.builder;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONObject;
import org.junit.Test;

import com.bluedot.efactura.model.TipoDoc;

/**
 * FchValor del e-Resguardo: se valida al crear el CFE con el formato exacto AAAA-MM-DD. Antes "2024-1-5" o
 * "2024-01-01x" pasaban la validacion y fallaban despues, al armar el XML, con el numero de CFE ya usado.
 */
public class DatosIdDocFchValorTest {

	/** Rechazado: sin aplicacion Play el APIException no arma su mensaje y sale otra excepcion; vale cualquiera */
	private static void rechaza(String fecha) {
		try {
			DatosIdDoc.leer(TipoDoc.eResguardo, new JSONObject().put("FchValor", fecha));
		} catch (Throwable e) {
			return;
		}
		fail("Se acepto FchValor=" + fecha);
	}

	@Test
	public void rechazaFormatosInvalidos() {
		rechaza("2024-1-5");
		rechaza("2024-01-01x");
		rechaza("2024-02-30");
		rechaza("01/02/2024");
	}

	/** Vacio = no enviado, como todos los campos de IdDoc */
	@Test
	public void vacioSeIgnora() throws Throwable {
		// sin datos que guardar leer devuelve null
		assertNull(DatosIdDoc.leer(TipoDoc.eResguardo, new JSONObject().put("FchValor", "")));
	}

	@Test
	public void aceptaFechaValida() throws Throwable {
		assertTrue(DatosIdDoc.leer(TipoDoc.eResguardo, new JSONObject().put("FchValor", "2024-01-05")).contains("2024-01-05"));
	}
}
