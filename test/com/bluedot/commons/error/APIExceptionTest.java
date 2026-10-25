package com.bluedot.commons.error;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.bluedot.commons.error.APIException.APIErrors;

public class APIExceptionTest {

	/** Lista vacia: "[]" (antes "]") */
	@Test
	public void detalleConListaVacia() {
		assertEquals("[]", APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage(Collections.<String> emptyList())
				.getDetailMessage());
	}

	@Test
	public void detalleConLista() {
		assertEquals("[a,b]", APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage(Arrays.asList("a", "b"))
				.getDetailMessage());
	}
}
