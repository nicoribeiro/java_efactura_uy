package dgi.classes.recepcion.wrappers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;

import org.junit.Test;

import dgi.classes.recepcion.ItemDetFact;

public class ItemDetFactWrapperTest {

	/** DescuentoPct sin DescuentoMonto (ej. sobre de Itau del 2026-10-14, que fallaba con NPE al recibirlo) */
	@Test
	public void descuentoPctSinMonto() {
		ItemDetFactWrapper item = new ItemDetFactWrapper(new ItemDetFact());
		item.setDescuentoPct(new BigDecimal("0.000"));
		assertEquals(new BigDecimal("0.000"), item.getDescuentoPct());
		assertNull(item.getDescuentoMonto());
	}

	@Test
	public void descuentoPctYMontoSeparados() {
		ItemDetFactWrapper item = new ItemDetFactWrapper(new ItemDetFact());
		item.setDescuentoPct(new BigDecimal("10"));
		item.setDescuentoMonto(new BigDecimal("25.50"));
		assertEquals(new BigDecimal("10"), item.getDescuentoPct());
		assertEquals(new BigDecimal("25.50"), item.getDescuentoMonto());
	}
}
