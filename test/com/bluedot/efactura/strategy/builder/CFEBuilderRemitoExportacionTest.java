package com.bluedot.efactura.strategy.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CFEDefType.ERemExp;
import dgi.classes.recepcion.IdDocRemExp;
import dgi.classes.recepcion.TipMonType;

/**
 * e-Remito de exportacion (124): items con IndFact opcional (5 u 8), totales sin IVA (MntExpoyAsim = MntTotal, puede
 * ser negativo), sin forma de pago, receptor del exterior y campos del IdDoc (TipoTraslado, ModVenta, ViaTransp,
 * propietario). No usa la base: receptor sin identificacion y sin referencias a CFE guardados.
 */
public class CFEBuilderRemitoExportacionTest {

	interface Accion {
		void run() throws APIException;
	}

	/** Ver CFEBuilderExportacionTest.rechaza */
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

	private static CFEBuilderRemitoExportacion builder() throws APIException {
		CFEStrategy strategy = new CFEStrategy.Builder().withTipo(TipoDoc.eRemito_de_Exportacion).build();
		return new CFEBuilderRemitoExportacion(null, strategy);
	}

	private static JSONObject item(Integer indFact, String cantidad, String precio) {
		JSONObject item = new JSONObject();
		item.put("NomItem", "Item " + indFact);
		if (indFact != null)
			item.put("IndFact", indFact);
		item.put("Cantidad", cantidad);
		item.put("UniMed", "UN");
		item.put("PrecioUnitario", precio);
		return item;
	}

	private static JSONObject receptor() {
		JSONObject receptor = new JSONObject();
		receptor.put("RznSocRecep", "ACME Inc.");
		receptor.put("DirRecep", "1 Main St");
		receptor.put("CiudadRecep", "Miami");
		receptor.put("DeptoRecep", "Florida");
		receptor.put("PaisRecep", "Estados Unidos");
		return receptor;
	}

	private static JSONObject idDoc() {
		JSONObject idDoc = new JSONObject();
		idDoc.put("FchEmis", "2026-10-16");
		idDoc.put("TipoTraslado", 1);
		idDoc.put("ModVenta", 1);
		idDoc.put("ViaTransp", 1);
		return idDoc;
	}

	private static JSONObject totales() {
		JSONObject totales = new JSONObject();
		totales.put("TpoMoneda", "USD");
		totales.put("TpoCambio", 40.5);
		return totales;
	}

	@Test
	public void factoryYEstrategia() throws APIException {
		CFEStrategy strategy = new CFEStrategy.Builder().withTipo(TipoDoc.eRemito_de_Exportacion_Contingencia).build();
		assertEquals(ERemExpStrategy.class, strategy.getClass());
	}

	@Test
	public void totalesSinIva() throws APIException {
		CFEBuilderRemitoExportacion b = builder();
		JSONArray detalle = new JSONArray();
		JSONObject bloques = item(null, "100", "25.50");
		bloques.put("NCM", "6810.11.00");
		bloques.put("CodItem", "BLQ-20");
		detalle.put(bloques);
		detalle.put(item(5, "10", "0"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totales(), false);
		JSONObject id = idDoc();
		// FmaPago no corresponde al e-Remito de exportacion: se ignora
		b.buildIdDoc(false, 1, id);
		b.buildReceptor(receptor());
		b.buildComplementoFiscal(null);

		ERemExp rem = b.getCFE().getEremitoExportacion();
		assertEquals(TipMonType.USD, rem.getEncabezado().getTotales().getTpoMoneda());
		assertEquals(new BigDecimal("40.500"), rem.getEncabezado().getTotales().getTpoCambio());
		assertEquals(new BigDecimal("2550.00"), rem.getEncabezado().getTotales().getMntExpoyAsim());
		assertEquals(new BigDecimal("2550.00"), rem.getEncabezado().getTotales().getMntTotal());
		assertEquals(2, rem.getEncabezado().getTotales().getCantLinDet());
		assertNull(rem.getDetalle().getItems().get(0).getIndFact());
		assertEquals(BigInteger.valueOf(5), rem.getDetalle().getItems().get(1).getIndFact());
		assertEquals(new BigDecimal("0.00"), rem.getDetalle().getItems().get(1).getMontoItem());
		assertEquals("6810.11.00", rem.getDetalle().getItems().get(0).getNCM());
		assertEquals("BLQ-20", rem.getDetalle().getItems().get(0).getCodItems().get(0).getCod());
		assertEquals("Estados Unidos", rem.getEncabezado().getReceptor().getPaisRecep());
		assertNull(b.getCFE().getFormaDePago());
		assertEquals(Double.valueOf(2550d), b.getCFE().getTotMntTotal());
		assertEquals(Double.valueOf(2550d), b.getCFE().getTotMntExpyAsim());
		assertEquals(Double.valueOf(0d), b.getCFE().getMntIVATasaBas());
		assertEquals(2, b.getCFE().getDetalle().size());
	}

	@Test
	public void itemsARebajarRestanYPuedenDarNegativo() throws APIException {
		CFEBuilderRemitoExportacion b = builder();
		JSONArray detalle = new JSONArray();
		detalle.put(item(null, "1", "100"));
		detalle.put(item(8, "3", "100"));
		b.buildDetalle(detalle, false);
		b.buildTotales(totales(), false);
		ERemExp rem = b.getCFE().getEremitoExportacion();
		assertEquals(new BigDecimal("-200.00"), rem.getEncabezado().getTotales().getMntExpoyAsim());
		assertEquals(new BigDecimal("-200.00"), rem.getEncabezado().getTotales().getMntTotal());

		// Con items a rebajar la referencia es obligatoria
		rechaza(APIErrors.MISSING_PARAMETER, () -> b.buildReferencia(null, null));
		JSONArray referencias = new JSONArray();
		JSONObject factura = new JSONObject();
		factura.put("TpoDocRef", 121);
		factura.put("Serie", "A");
		factura.put("NroCFERef", 1);
		factura.put("NroLinRef", 1);
		referencias.put(factura);
		// ... y tiene que ser a un e-Remito de exportacion (o global)
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildReferencia(null, referencias));
	}

	@Test
	public void rechazaItemsInvalidos() throws APIException {
		Object[][] casos = {
				// IndFact de la e-Factura
				{ item(10, "1", "100"), APIErrors.BAD_PARAMETER_VALUE },
				// Sin indicador el precio tiene que ser mayor que cero
				{ item(null, "1", "0"), APIErrors.BAD_PARAMETER_VALUE },
				// Entrega gratuita con precio
				{ item(5, "1", "10"), APIErrors.BAD_PARAMETER_VALUE },
				// A rebajar con precio negativo
				{ item(8, "1", "-1"), APIErrors.BAD_PARAMETER_VALUE },
				// Sin precio unitario (B-C11 obligatorio)
				{ new JSONObject(item(null, "1", "1"), new String[] { "NomItem", "Cantidad", "UniMed" }), APIErrors.MISSING_PARAMETER }, };
		for (Object[] caso : casos) {
			JSONArray detalle = new JSONArray();
			detalle.put(caso[0]);
			CFEBuilderRemitoExportacion b = builder();
			rechaza((APIErrors) caso[1], () -> b.buildDetalle(detalle, false));
		}

		// Descuentos: no corresponden
		JSONObject descuento = item(null, "1", "100");
		descuento.put("DescuentoPct", "10");
		JSONArray detalle = new JSONArray();
		detalle.put(descuento);
		CFEBuilderRemitoExportacion b = builder();
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildDetalle(detalle, false));

		// Sin lineas
		CFEBuilderRemitoExportacion vacio = builder();
		rechaza(APIErrors.MISSING_PARAMETER, () -> vacio.buildDetalle(new JSONArray(), false));
	}

	@Test
	public void receptorObligatorioSalvoSecretoProfesional() throws APIException {
		CFEBuilderRemitoExportacion b = builder();
		b.buildIdDoc(false, null, idDoc());
		JSONObject receptor = receptor();
		receptor.remove("DeptoRecep");
		rechaza(APIErrors.MISSING_PARAMETER, () -> b.buildReceptor(receptor));

		CFEBuilderRemitoExportacion secreto = builder();
		JSONObject id = idDoc();
		id.put("SecProf", 1);
		secreto.buildIdDoc(false, null, id);
		secreto.buildReceptor(new JSONObject());
		// Sin datos el receptor no va en el XML (es opcional en eRem_Exp)
		assertNull(secreto.getCFE().getEremitoExportacion().getEncabezado());
	}

	@Test
	public void complementoFiscalNoCorresponde() throws APIException {
		CFEBuilderRemitoExportacion b = builder();
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildComplementoFiscal(new JSONObject()));
	}

	@Test
	public void datosIdDoc() throws APIException {
		JSONObject completo = idDoc();
		completo.put("ClauVenta", "cif");
		completo.put("IndPropiedad", 1);
		completo.put("TipoDocProp", 5);
		completo.put("CodPaisProp", "ar");
		completo.put("DocPropExt", "AB123456");
		completo.put("RznSocProp", "Duenio SA");
		completo.put("InfoAdicionalDoc", "Contenedor MSCU1234567");
		String datos = DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, completo);
		IdDocRemExp id = new IdDocRemExp();
		DatosIdDoc.aplicar(datos, id);
		assertEquals(BigInteger.ONE, id.getTipoTraslado());
		assertEquals(1, id.getModVenta());
		assertEquals(1, id.getViaTransp());
		assertEquals("CIF", id.getClauVenta());
		assertEquals(BigInteger.ONE, id.getIndPropiedad());
		assertEquals(Integer.valueOf(5), id.getTipoDocProp());
		assertEquals("AR", id.getCodPaisProp());
		assertEquals("AB123456", id.getDocPropExt());
		assertNull(id.getDocProp());
		assertEquals("Duenio SA", id.getRznSocProp());

		// ClauVenta es opcional en el e-Remito de exportacion (A-C13 = 3)
		IdDocRemExp sinClausula = new IdDocRemExp();
		DatosIdDoc.aplicar(DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, idDoc()), sinClausula);
		assertNull(sinClausula.getClauVenta());

		// TipoTraslado, ModVenta y ViaTransp obligatorios
		for (String campo : new String[] { "TipoTraslado", "ModVenta", "ViaTransp" }) {
			JSONObject falta = idDoc();
			falta.remove(campo);
			rechaza(APIErrors.MISSING_PARAMETER, () -> DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, falta));
		}

		// Valores invalidos
		Object[][] invalidos = { { "TipoTraslado", 3 }, { "ModVenta", 80 }, { "ViaTransp", 4 }, { "SecProf", 2 }, { "IVAalDia", 1 },
				{ "IndPropiedad", 2 }, { "TipoDocProp", 1 }, { "CodPaisProp", "URU" } };
		for (Object[] invalido : invalidos) {
			JSONObject mal = idDoc();
			mal.put((String) invalido[0], invalido[1]);
			rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, mal));
		}

		// Propietario incompleto o incoherente
		JSONObject sinNombre = idDoc();
		sinNombre.put("IndPropiedad", 1);
		sinNombre.put("TipoDocProp", 2);
		sinNombre.put("CodPaisProp", "UY");
		sinNombre.put("DocProp", "219999990016");
		rechaza(APIErrors.MISSING_PARAMETER, () -> DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, sinNombre));
		JSONObject rucExtranjero = idDoc();
		rucExtranjero.put("TipoDocProp", 2);
		rucExtranjero.put("CodPaisProp", "AR");
		rucExtranjero.put("DocProp", "219999990016");
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, rucExtranjero));
		JSONObject pasaporteSinDoc = idDoc();
		pasaporteSinDoc.put("TipoDocProp", 5);
		pasaporteSinDoc.put("CodPaisProp", "US");
		rechaza(APIErrors.MISSING_PARAMETER, () -> DatosIdDoc.leer(TipoDoc.eRemito_de_Exportacion, pasaporteSinDoc));

		// Los campos del propietario se siguen ignorando en los demas tipos (como antes)
		JSONObject efactura = new JSONObject();
		efactura.put("TipoTraslado", 1);
		efactura.put("RznSocProp", "X");
		assertNull(DatosIdDoc.leer(TipoDoc.eFactura, efactura));
	}
}
