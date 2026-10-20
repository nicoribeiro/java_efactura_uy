package com.bluedot.efactura.strategy.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CFEDefType.EBoleta;
import dgi.classes.recepcion.TipMonType;

/**
 * e-Boleta de entrada: totales sin IVA, indicadores de facturacion admitidos y su relacion con el vendedor y con la
 * compra de moneda extranjera para su reventa. No usa la base: los casos con vendedor identificado se rechazan antes de
 * buscar el pais o la empresa.
 */
public class CFEBuilderBoletaTest {

	interface Accion {
		void run() throws APIException;
	}

	/**
	 * Que la accion se rechace con el error esperado. Sin aplicacion de Play levantada APIException.withParams no puede leer
	 * los mensajes y tira RuntimeException: en ese caso alcanza con que la excepcion salga de APIException
	 */
	private static void rechaza(APIErrors esperado, Accion accion) {
		try {
			accion.run();
		} catch (APIException e) {
			assertEquals(esperado, e.getError());
			return;
		} catch (RuntimeException e) {
			for (StackTraceElement el : e.getStackTrace())
				if (el.getClassName().equals(APIException.class.getName()))
					return;
			throw e;
		}
		fail("no se rechazo");
	}

	private static CFEBuilderBoleta builder(TipoDoc tipo) throws APIException {
		CFEStrategy strategy = new CFEStrategy.Builder().withTipo(tipo).build();
		return new CFEBuilderBoleta(null, strategy);
	}

	private static JSONObject item(int indFact, String cantidad, String precio) {
		JSONObject item = new JSONObject();
		item.put("NomItem", "Item " + indFact);
		item.put("IndFact", indFact);
		item.put("Cantidad", cantidad);
		item.put("UniMed", "N/A");
		item.put("PrecioUnitario", precio);
		return item;
	}

	private static JSONObject totalesUYU() {
		JSONObject totales = new JSONObject();
		totales.put("TpoMoneda", "UYU");
		return totales;
	}

	private static JSONObject idDoc() {
		JSONObject idDoc = new JSONObject();
		idDoc.put("FchEmis", "2026-10-18");
		return idDoc;
	}

	private static JSONObject vendedorRuc() {
		JSONObject receptor = new JSONObject();
		receptor.put("TipoDocRecep", 2);
		receptor.put("CodPaisRecep", "UY");
		receptor.put("DocRecep", "219999990016");
		receptor.put("RznSocRecep", "VENDEDOR SA");
		return receptor;
	}

	private static JSONObject vendedorCi() {
		JSONObject receptor = new JSONObject();
		receptor.put("TipoDocRecep", 3);
		receptor.put("CodPaisRecep", "UY");
		receptor.put("DocRecep", "12345672");
		receptor.put("RznSocRecep", "Juan Perez");
		return receptor;
	}

	@Test
	public void totalesSinIva() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(13, "2", "500.25"));
		detalle.put(item(12, "1", "200"));
		detalle.put(item(6, "1", "30"));
		detalle.put(item(7, "1", "10"));
		detalle.put(item(5, "1", "99"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		b.buildIdDoc(false, 1, idDoc());

		EBoleta boleta = b.getCFE().getEboleta();
		assertEquals(TipMonType.UYU, boleta.getEncabezado().getTotales().getTpoMoneda());
		assertEquals(new BigDecimal("1000.50"), boleta.getEncabezado().getTotales().getMntNoGrv());
		assertEquals(new BigDecimal("200.00"), boleta.getEncabezado().getTotales().getMntIVaenSusp());
		assertEquals(new BigDecimal("1200.50"), boleta.getEncabezado().getTotales().getMntTotal());
		assertEquals(new BigDecimal("20.00"), boleta.getEncabezado().getTotales().getMontoNF());
		assertEquals(new BigDecimal("1220.50"), boleta.getEncabezado().getTotales().getMntPagar());
		assertEquals(5, boleta.getEncabezado().getTotales().getCantLinDet());
		assertNull(boleta.getEncabezado().getTotales().getMntTotRetenido());
		assertEquals(Double.valueOf(1000.50d), b.getCFE().getTotMntNoGrv());
		assertEquals(Double.valueOf(200d), b.getCFE().getTotMntIVAenSusp());
		assertEquals(Double.valueOf(0d), b.getCFE().getMntIVATasaBas());
	}

	@Test
	public void sinMontosEnCeroEnElXml() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(14, "1", "100"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);

		EBoleta boleta = b.getCFE().getEboleta();
		assertEquals(new BigDecimal("100.00"), boleta.getEncabezado().getTotales().getMntNoGrv());
		assertNull(boleta.getEncabezado().getTotales().getMntIVaenSusp());
		assertNull(boleta.getEncabezado().getTotales().getMontoNF());
		assertEquals(new BigDecimal("100.00"), boleta.getEncabezado().getTotales().getMntPagar());
	}

	@Test
	public void rechazaIndFactNoAdmitido() throws APIException {
		for (int indFact : new int[] { 1, 2, 3, 10, 16 }) {
			CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
			JSONArray detalle = new JSONArray();
			detalle.put(item(indFact, "1", "100"));
			rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildDetalle(detalle, false));
		}
	}

	@Test
	public void rechazaRetenciones() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		JSONObject item = item(15, "1", "100");
		item.put("RetencPercep", new JSONArray());
		detalle.put(item);
		rechaza(APIErrors.NOT_SUPPORTED, () -> b.buildDetalle(detalle, false));
	}

	@Test
	public void compraMEReventaSoloIndFact17() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(17, "100", "40.5"));
		detalle.put(item(13, "1", "10"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		JSONObject idDoc = idDoc();
		idDoc.put("IndCompraMEReventa", 1);
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildIdDoc(false, 1, idDoc));
	}

	@Test
	public void indFact17SinCompraMEReventa() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(17, "100", "40.5"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildIdDoc(false, 1, idDoc()));
	}

	@Test
	public void vendedorObligatorio() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(13, "1", "100"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		b.buildIdDoc(false, 1, idDoc());
		rechaza(APIErrors.MISSING_PARAMETER, () -> b.buildReceptor(new JSONObject()));
	}

	@Test
	public void noContribuyenteSinRuc() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(13, "1", "100"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		b.buildIdDoc(false, 1, idDoc());
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildReceptor(vendedorRuc()));
	}

	@Test
	public void ivaMinimoExigeRuc() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		JSONArray detalle = new JSONArray();
		detalle.put(item(14, "1", "100"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totalesUYU(), false);
		b.buildIdDoc(false, 1, idDoc());
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildReceptor(vendedorCi()));
	}

	@Test
	public void sinComplementoFiscal() throws APIException {
		CFEBuilderBoleta b = builder(TipoDoc.eBoleta_de_entrada);
		rechaza(APIErrors.NOT_SUPPORTED, () -> b.buildComplementoFiscal(new JSONObject()));
	}

	@Test
	public void datosIdDoc() throws APIException {
		JSONObject idDoc = idDoc();
		idDoc.put("IndCompraMEReventa", 1);
		idDoc.put("InfoAdicionalDoc", "Compra de USD");
		JSONObject datos = new JSONObject(DatosIdDoc.leer(TipoDoc.eBoleta_de_entrada, idDoc));
		assertEquals(1, datos.getInt("IndCompraMEReventa"));
		assertEquals("Compra de USD", datos.getString("InfoAdicionalDoc"));

		JSONObject porCuentaAjena = idDoc();
		porCuentaAjena.put("IndCompraMEReventa", 2);
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eBoleta_de_entrada, porCuentaAjena));

		JSONObject modVenta = idDoc();
		modVenta.put("ModVenta", 1);
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eBoleta_de_entrada, modVenta));
	}

}
