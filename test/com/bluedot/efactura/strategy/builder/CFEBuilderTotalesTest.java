package com.bluedot.efactura.strategy.builder;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.bluedot.efactura.model.IndicadorFacturacion;

import dgi.classes.recepcion.ItemDetFact;
import dgi.classes.recepcion.wrappers.ItemDetFactWrapper;
import dgi.classes.recepcion.wrappers.ItemInterface;

/**
 * Totales de e-Factura / e-Ticket: se suma MontoItem, que ya tiene el descuento del item. Antes se sumaba
 * PrecioUnitario x Cantidad y el neto y el IVA quedaban sin el descuento.
 */
public class CFEBuilderTotalesTest {

	private static ItemInterface item(int indFact, String precio, String cantidad, String montoItem) {
		ItemInterface item = new ItemDetFactWrapper(new ItemDetFact());
		item.setIndFact(BigInteger.valueOf(indFact));
		item.setPrecioUnitario(new BigDecimal(precio));
		item.setCantidad(new BigDecimal(cantidad));
		item.setMontoItem(new BigDecimal(montoItem));
		return item;
	}

	@Test
	public void sumaMontoItemConDescuento() {
		int basica = IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA.getIndice();
		List<ItemInterface> items = new ArrayList<>();
		items.add(item(basica, "100", "2", "150"));
		items.add(item(basica, "10", "1", "10"));
		BigDecimal[] montos = CFEBuilderImpl.montosPorIndicador(items);
		assertEquals(0, new BigDecimal("160").compareTo(montos[basica]));
	}

	@Test
	public void separaPorIndicador() {
		int basica = IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_BASICA.getIndice();
		int minima = IndicadorFacturacion.INDICADOR_FACTURACION_IVA_TASA_MINIMA.getIndice();
		List<ItemInterface> items = new ArrayList<>();
		items.add(item(basica, "100", "1", "100"));
		items.add(item(minima, "50", "2", "100"));
		BigDecimal[] montos = CFEBuilderImpl.montosPorIndicador(items);
		assertEquals(0, new BigDecimal("100").compareTo(montos[basica]));
		assertEquals(0, new BigDecimal("100").compareTo(montos[minima]));
		assertEquals(0, BigDecimal.ZERO.compareTo(montos[IndicadorFacturacion.INDICADOR_FACTURACION_EXCENTO_IVA.getIndice()]));
	}
}
