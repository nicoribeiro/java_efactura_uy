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

import dgi.classes.recepcion.CFEDefType.EFactExp;
import dgi.classes.recepcion.IdDocFactExp;
import dgi.classes.recepcion.TipMonType;

/**
 * e-Factura de exportacion: totales sin IVA, indicadores de facturacion admitidos, receptor extranjero y campos del IdDoc
 * (ClauVenta, ModVenta, ViaTransp). No usa la base: receptor sin identificacion (no busca pais ni empresa).
 */
public class CFEBuilderExportacionTest {

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

	private static CFEBuilderExportacion builder(TipoDoc tipo) throws APIException {
		CFEStrategy strategy = new CFEStrategy.Builder().withTipo(tipo).build();
		return new CFEBuilderExportacion(null, strategy);
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
		idDoc.put("FchEmis", "2026-10-15");
		idDoc.put("ClauVenta", "fob");
		idDoc.put("ModVenta", 1);
		idDoc.put("ViaTransp", 1);
		return idDoc;
	}

	@Test
	public void totalesSinIva() throws APIException {
		CFEBuilderExportacion b = builder(TipoDoc.eFactura_Exportacion);
		JSONArray detalle = new JSONArray();
		detalle.put(item(10, "2", "1000.50"));
		JSONObject nf = item(6, "1", "30");
		nf.put("NCM", "0201.30.00");
		detalle.put(nf);
		detalle.put(item(7, "1", "10"));
		b.buildDetalle(detalle, false);

		JSONObject totales = new JSONObject();
		totales.put("TpoMoneda", "USD");
		totales.put("TpoCambio", 40.123);
		b.buildTotales(totales, false);
		b.buildIdDoc(false, 1, idDoc());
		b.buildReceptor(receptor());

		EFactExp exp = b.getCFE().getEfacturaExportacion();
		assertEquals(TipMonType.USD, exp.getEncabezado().getTotales().getTpoMoneda());
		assertEquals(new BigDecimal("40.123"), exp.getEncabezado().getTotales().getTpoCambio());
		assertEquals(new BigDecimal("2001.00"), exp.getEncabezado().getTotales().getMntExpoyAsim());
		assertEquals(new BigDecimal("2001.00"), exp.getEncabezado().getTotales().getMntTotal());
		assertEquals(new BigDecimal("20.00"), exp.getEncabezado().getTotales().getMontoNF());
		assertEquals(new BigDecimal("2021.00"), exp.getEncabezado().getTotales().getMntPagar());
		assertEquals(3, exp.getEncabezado().getTotales().getCantLinDet());
		assertEquals("0201.30.00", exp.getDetalle().getItems().get(1).getNCM());
		assertEquals(Double.valueOf(0d), b.getCFE().getMntIVATasaBas());
		assertEquals("Estados Unidos", exp.getEncabezado().getReceptor().getPaisRecep());
		assertNull(exp.getEncabezado().getReceptor().getTipoDocRecep());
	}

	@Test
	public void rechazaIndFactConIva() throws APIException {
		CFEBuilderExportacion b = builder(TipoDoc.eFactura_Exportacion);
		JSONArray detalle = new JSONArray();
		detalle.put(item(3, "1", "100"));
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildDetalle(detalle, false));
	}

	@Test
	public void receptorSinPais() throws APIException {
		CFEBuilderExportacion b = builder(TipoDoc.Nota_de_Credito_de_eFactura_Exportacion);
		b.buildIdDoc(false, 1, idDoc());
		JSONObject receptor = receptor();
		receptor.remove("PaisRecep");
		rechaza(APIErrors.MISSING_PARAMETER, () -> b.buildReceptor(receptor));
	}

	@Test
	public void receptorConSecretoProfesional() throws APIException {
		CFEBuilderExportacion b = builder(TipoDoc.eFactura_Exportacion);
		JSONObject idDoc = idDoc();
		idDoc.put("SecProf", 1);
		b.buildIdDoc(false, 1, idDoc);
		b.buildReceptor(new JSONObject());
	}

	@Test
	public void receptorDocumentoIncompleto() throws APIException {
		CFEBuilderExportacion b = builder(TipoDoc.eFactura_Exportacion);
		b.buildIdDoc(false, 1, idDoc());
		JSONObject receptor = receptor();
		receptor.put("TipoDocRecep", 7);
		receptor.put("CodPaisRecep", "US");
		rechaza(APIErrors.MISSING_PARAMETER, () -> b.buildReceptor(receptor));
		receptor.put("DocRecepExt", "12-3456789");
		receptor.put("DocRecep", "211234560011");
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildReceptor(receptor));
		receptor.put("TipoDocRecep", 6);
		receptor.remove("DocRecep");
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> b.buildReceptor(receptor));
	}

	@Test
	public void datosIdDoc() throws APIException {
		String datos = DatosIdDoc.leer(TipoDoc.eFactura_Exportacion, idDoc());
		IdDocFactExp id = new IdDocFactExp();
		DatosIdDoc.aplicar(datos, id);
		assertEquals("FOB", id.getClauVenta());
		assertEquals(Integer.valueOf(1), id.getModVenta());
		assertEquals(1, id.getViaTransp());

		// Notas: ModVenta opcional
		JSONObject nota = idDoc();
		nota.remove("ModVenta");
		DatosIdDoc.leer(TipoDoc.Nota_de_Credito_de_eFactura_Exportacion, nota);
		DatosIdDoc.leer(TipoDoc.Nota_de_Debito_de_eFactura_Exportacion_Contingencia, nota);

		// e-Factura de exportacion: ModVenta, ClauVenta y ViaTransp obligatorios
		for (String campo : new String[] { "ModVenta", "ClauVenta", "ViaTransp" }) {
			JSONObject falta = idDoc();
			falta.remove(campo);
			rechaza(APIErrors.MISSING_PARAMETER, () -> DatosIdDoc.leer(TipoDoc.eFactura_Exportacion, falta));
		}

		// Valores invalidos: ModVenta 80 (Tax Free, solo e-Ticket), ViaTransp 4, ClauVenta de mas de 3 letras, IVAalDia 2 (mandante)
		Object[][] invalidos = { { "ModVenta", 80 }, { "ViaTransp", 4 }, { "ClauVenta", "FOBX" }, { "IVAalDia", 2 } };
		for (Object[] invalido : invalidos) {
			JSONObject mal = idDoc();
			mal.put((String) invalido[0], invalido[1]);
			rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eFactura_Exportacion, mal));
		}

		// ClauVenta / ViaTransp no corresponden a la e-Factura
		JSONObject efactura = new JSONObject();
		efactura.put("ViaTransp", 1);
		rechaza(APIErrors.BAD_PARAMETER_VALUE, () -> DatosIdDoc.leer(TipoDoc.eFactura, efactura));

		// N/A es valido
		JSONObject na = idDoc();
		na.put("ClauVenta", "n/a");
		DatosIdDoc.aplicar(DatosIdDoc.leer(TipoDoc.eFactura_Exportacion, na), id);
		assertEquals("N/A", id.getClauVenta());
	}

	@Test
	public void remitoDeExportacionTieneSuBuilder() throws APIException {
		CFEStrategy strategy = new CFEStrategy.Builder().withTipo(TipoDoc.eRemito_de_Exportacion).build();
		assertEquals(ERemExpStrategy.class, strategy.getClass());
		// e-Remito (181) sigue sin envio a DGI, pero el builder existe
		CFEStrategy remito = new CFEStrategy.Builder().withTipo(TipoDoc.eRemito).build();
		assertEquals(ERemitoStrategy.class, remito.getClass());
	}
}
