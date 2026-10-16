package com.bluedot.efactura.recibidos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.bluedot.commons.utils.XML;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.serializers.CFEEmpresasTypeSerializer;
import com.bluedot.efactura.serializers.CFEEmpresasTypeWrapper;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderExportacion;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;
import com.bluedot.efactura.strategy.builder.CFEBuilderRemitoExportacion;

import dgi.classes.entreEmpresas.CFEEmpresasType;
import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.recepcion.CFEDefType;
import dgi.classes.recepcion.TipMonType;

/**
 * Recepcion de CFE de exportacion (e-Factura de exportacion 121 y e-Remito de exportacion 124) dentro de un sobre
 * EnvioCFE_entreEmpresas: el XML se pasa a JSON con CFEEmpresasTypeSerializer (antes no habia estrategia y daba
 * NullPointerException) y el JSON lo arma el builder de emision. Sin base de datos: no se arman el emisor ni el
 * receptor (buscan o crean la empresa) ni se guarda el CFE; eso queda para la prueba de punta a punta en test.
 */
public class RecepcionExportacionTest {

	private static CFEEmpresasType leer(String archivo) throws Throwable {
		EnvioCFEEntreEmpresas envio = (EnvioCFEEntreEmpresas) XML.unMarshall(XML.readDocument("resources/ejemplos/" + archivo),
				EnvioCFEEntreEmpresas.class);
		assertEquals(1, envio.getCFEAdendas().size());
		return envio.getCFEAdendas().get(0);
	}

	private static JSONObject json(CFEEmpresasType cfe) {
		return new CFEEmpresasTypeSerializer<CFEEmpresasTypeWrapper>().objectToJson(new CFEEmpresasTypeWrapper(cfe, null));
	}

	/** Lo que hace CFEMicroControllerDefault.create con un CFE recibido, salvo emisor, receptor y referencias (base) */
	private static CFE armar(CFEBuiderInterface builder, JSONObject json) throws Throwable {
		JSONObject encabezado = json.getJSONObject("Encabezado");
		builder.buildTimestampFirma(json.getLong("TmstFirma"));
		builder.buildDetalle(json.getJSONArray("Detalle"), false);
		builder.buildTotales(encabezado.getJSONObject("Totales"), false);
		JSONObject idDoc = encabezado.getJSONObject("IdDoc");
		builder.buildIdDoc(false, idDoc.has("FmaPago") ? idDoc.getInt("FmaPago") : null, idDoc);
		builder.buildComplementoFiscal(json.has("CompFiscal") ? json.getJSONObject("CompFiscal") : null);
		return builder.getCFE();
	}

	@Test
	public void eFacturaDeExportacion() throws Throwable {
		CFEEmpresasType cfe = leer("Sob_219999990016_20261016_7001.xml");

		JSONObject json = json(cfe);
		JSONObject idDoc = json.getJSONObject("Encabezado").getJSONObject("IdDoc");
		assertEquals(121, idDoc.getInt("TipoCFE"));
		assertEquals("FOB", idDoc.getString("ClauVenta"));
		assertEquals(1, idDoc.getInt("ModVenta"));
		assertEquals(3, idDoc.getInt("ViaTransp"));
		assertEquals(2, idDoc.getInt("FmaPago"));
		assertEquals("EXP-2026-0015", idDoc.getString("NroInterno"));
		JSONObject receptor = json.getJSONObject("Encabezado").getJSONObject("Receptor");
		assertEquals("218757590018", receptor.getString("DocRecep"));
		assertEquals(2, receptor.getInt("TipoDocRecep"));
		assertEquals("Uruguay", receptor.getString("PaisRecep"));
		assertEquals("Zonamerica, deposito 3", receptor.getString("LugarDestEnt"));
		assertEquals("219999990016", json.getJSONObject("Encabezado").getJSONObject("Emisor").getString("RUCEmisor"));
		JSONArray detalle = json.getJSONArray("Detalle");
		assertEquals(2, detalle.length());
		assertEquals("6810.11.00", detalle.getJSONObject(0).getString("NCM"));
		assertEquals("BLQ-20", detalle.getJSONObject(0).getString("CodItem"));
		assertFalse(detalle.getJSONObject(0).has("DscItem"));
		assertFalse(json.has("Referencia"));

		CFEBuiderInterface builder = CFEBuilderFactory.getCFEBuilder(cfe);
		assertTrue(builder instanceof CFEBuilderExportacion);
		CFE modelo = armar(builder, json);
		assertEquals(TipoDoc.eFactura_Exportacion, modelo.getTipo());
		assertEquals(15L, modelo.getNro());
		assertEquals("A", modelo.getSerie());
		assertEquals(TipMonType.USD, modelo.getMoneda());
		assertEquals(40.125d, modelo.getTipoCambio(), 0.0001);
		assertEquals(2700d, modelo.getTotMntTotal(), 0.001);
		assertEquals("6810.11.00", modelo.getEfacturaExportacion().getDetalle().getItems().get(0).getNCM());

		// Montos del XML del emisor (MontosRecibidos): iguales a los calculados, IVA en cero
		CFEDefType xml = cfe.getCFE();
		assertTrue(MontosRecibidos.aplicar(modelo, xml, true).isEmpty());
		assertEquals(2700d, modelo.getTotMntExpyAsim(), 0.001);
		assertEquals(0d, modelo.getMntIVATasaBas(), 0.001);
	}

	@Test
	public void eRemitoDeExportacion() throws Throwable {
		CFEEmpresasType cfe = leer("Sob_219999990016_20261016_7002.xml");

		JSONObject json = json(cfe);
		JSONObject idDoc = json.getJSONObject("Encabezado").getJSONObject("IdDoc");
		assertEquals(124, idDoc.getInt("TipoCFE"));
		assertEquals(1, idDoc.getInt("TipoTraslado"));
		assertFalse(idDoc.has("FmaPago"));
		JSONArray detalle = json.getJSONArray("Detalle");
		assertFalse(detalle.getJSONObject(0).has("IndFact"));
		assertEquals("5", detalle.getJSONObject(1).getString("IndFact"));

		CFEBuiderInterface builder = CFEBuilderFactory.getCFEBuilder(cfe);
		assertTrue(builder instanceof CFEBuilderRemitoExportacion);
		CFE modelo = armar(builder, json);
		assertEquals(TipoDoc.eRemito_de_Exportacion, modelo.getTipo());
		assertEquals(8L, modelo.getNro());
		assertEquals(null, modelo.getFormaDePago());
		assertEquals(new BigDecimal("2550.00"), modelo.getEremitoExportacion().getEncabezado().getTotales().getMntTotal());
		assertEquals(2, modelo.getDetalle().size());
		assertEquals("Pallets retornables, sin cargo", modelo.getDetalle().get(1).getDescripcionItem());

		// Si el emisor redondeo distinto, mandan los Totales de su XML
		cfe.getCFE().getERemExp().getEncabezado().getTotales().setMntTotal(new BigDecimal("2550.01"));
		assertEquals(1, MontosRecibidos.aplicar(modelo, cfe.getCFE(), true).size());
		assertEquals(2550.01d, modelo.getTotMntTotal(), 0.0001);
	}
}
