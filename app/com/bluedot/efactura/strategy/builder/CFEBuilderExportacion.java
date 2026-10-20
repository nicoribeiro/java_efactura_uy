package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.IndicadorFacturacion;
import com.bluedot.efactura.model.TipoDocumento;

import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * Builder de la e-Factura de Exportacion y sus notas (121-123, 221-223); CFEBuilderRemitoExportacion lo extiende para el
 * e-Remito de exportacion (124, 224). Diferencias con la e-Factura (Formato CFE v25-2):
 * <ul>
 * <li>Receptor extranjero (A-C60 a A-C70): no se exige RUC ni pais UY; nombre, direccion, ciudad, departamento y pais
 * son obligatorios salvo secreto profesional</li>
 * <li>Items: solo indicadores de facturacion 5 (entrega gratuita), 6 y 7 (no facturable) y 10 (exportacion y
 * asimiladas), segun Item_Det_Fact_Exp del XSD; admite NCM (B-C7.1)</li>
 * <li>Totales sin IVA (Totales_Fact_Exp): MntExpoyAsim (A-C113) = suma de MontoItem con IndFact 10, MntTotal (A-C124) =
 * MntExpoyAsim, MontoNF (A-C129) = IndFact 6 menos IndFact 7, MntPagar (A-C130) = MntTotal + MontoNF</li>
 * </ul>
 */
public class CFEBuilderExportacion extends CFEBuilderImpl implements CFEBuiderInterface {

	/** Secreto profesional (A-C18) en el IdDoc: con el, los datos del receptor no son obligatorios */
	protected boolean secretoProfesional = false;

	public CFEBuilderExportacion(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}

	protected EstrategiaExportacion getStrategy() {
		return (EstrategiaExportacion) strategy;
	}

	@Override
	public void buildIdDoc(boolean montosIncluyenIva, Integer formaPago, JSONObject idDocJson) throws APIException {
		// La exportacion no lleva IVA: MntBruto no corresponde
		super.buildIdDoc(false, formaPago, idDocJson);
		secretoProfesional = idDocJson.has("SecProf") && !idDocJson.isNull("SecProf") && "1".equals(idDocJson.get("SecProf").toString().trim());
	}

	protected static String texto(JSONObject json, String campo) {
		if (!json.has(campo) || json.isNull(campo))
			return null;
		String valor = Commons.textoJson(json.get(campo)).trim();
		return valor.isEmpty() ? null : valor;
	}

	@Override
	public void buildReceptor(JSONObject receptorJson) throws APIException {

		TipoDocumento tipoDocRecep = null;
		if (texto(receptorJson, "TipoDocRecep") != null) {
			try {
				tipoDocRecep = TipoDocumento.fromInt(Integer.parseInt(texto(receptorJson, "TipoDocRecep")));
			} catch (NumberFormatException e) {
			}
			if (tipoDocRecep == null)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDocRecep", receptorJson.get("TipoDocRecep"))
						.setDetailMessage("TipoDocRecep admite 1 NIE, 2 RUC, 3 CI, 4 otros, 5 pasaporte, 6 DNI, 7 NIFE");
		}

		Integer cp = null;
		if (texto(receptorJson, "CP") != null) {
			try {
				cp = Integer.parseInt(texto(receptorJson, "CP"));
			} catch (NumberFormatException e) {
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CP", receptorJson.get("CP"))
						.setDetailMessage("CP tiene que ser numerico de hasta 5 digitos");
			}
		}

		// Mismo filtro de direcciones de correo que CFEBuilderImpl.buildReceptor
		String mailPdfAddress = texto(receptorJson, "MailPdfAddress");
		if (mailPdfAddress != null) {
			StringBuilder result = new StringBuilder();
			for (String mail : mailPdfAddress.split("[\\s,]+")) {
				if (mail.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
					result.append(mail);
					result.append(",");
				}
			}
			mailPdfAddress = result.length() > 0 ? result.substring(0, result.length() - 1) : "";
		}

		getStrategy().buildReceptor(tipoDocRecep, texto(receptorJson, "CodPaisRecep"), texto(receptorJson, "DocRecep"),
				texto(receptorJson, "DocRecepExt"), texto(receptorJson, "RznSocRecep"), texto(receptorJson, "DirRecep"),
				texto(receptorJson, "CiudadRecep"), texto(receptorJson, "DeptoRecep"), texto(receptorJson, "PaisRecep"), cp,
				texto(receptorJson, "InfoAdicional"), texto(receptorJson, "LugarDestEnt"), mailPdfAddress,
				texto(receptorJson, "CompraID"), secretoProfesional);
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		// Indicadores de facturacion admitidos en Item_Det_Fact_Exp: antes de armar las lineas
		for (int i = 0; i < detalleJson.length(); i++) {
			JSONObject itemJson = detalleJson.getJSONObject(i);
			String indFact = texto(itemJson, "IndFact");
			if (indFact == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle[" + (i + 1) + "].IndFact");
			if (!indFact.equals("5") && !indFact.equals("6") && !indFact.equals("7") && !indFact.equals("10"))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + (i + 1) + ": en la e-Factura de exportacion IndFact admite 10 (exportacion y asimiladas), "
								+ "5 (entrega gratuita), 6 (no facturable) o 7 (no facturable negativo)");
			String ncm = texto(itemJson, "NCM");
			if (ncm != null && ncm.length() > 20)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("NCM", ncm)
						.setDetailMessage("Linea " + (i + 1) + ": NCM admite hasta 20 caracteres");
		}

		super.buildDetalle(detalleJson, false);

		aplicarNCM(detalleJson);
	}

	/** NCM (B-C7.1, posicion arancelaria): opcional, no esta en ItemInterface */
	protected void aplicarNCM(JSONArray detalleJson) {
		int cantidad = strategy.getItem().size();
		for (int i = 0; i < detalleJson.length() && i < cantidad; i++) {
			String ncm = texto(detalleJson.getJSONObject(i), "NCM");
			if (ncm != null)
				getStrategy().setNCM(i, ncm);
		}
	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException {
		List<ItemInterface> items = strategy.getItem();

		TotalesInterface totales = strategy.getTotales();

		monedaYTipoDeCambio(totalesJson, totales);

		/*
		 * Cantidad de lineas (A-C126)
		 */
		totales.setCantLinDet(items.size());

		/*
		 * Suma de MontoItem (B-C24) por indicador de facturacion
		 */
		BigDecimal exportacion = BigDecimal.ZERO;
		BigDecimal noFacturable = BigDecimal.ZERO;
		BigDecimal noFacturableNegativo = BigDecimal.ZERO;

		for (ItemInterface item : items) {
			int indFact = item.getIndFact().intValue();
			if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_EXPORTACION_Y_ASIMILADAS.getIndice())
				exportacion = exportacion.add(item.getMontoItem());
			else if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE.getIndice())
				noFacturable = noFacturable.add(item.getMontoItem());
			else if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO.getIndice())
				noFacturableNegativo = noFacturableNegativo.add(item.getMontoItem());
			// 5 (entrega gratuita): no suma
		}

		/*
		 * id:113 Total Monto - Exportacion y asimiladas
		 */
		BigDecimal mntExpoyAsim = exportacion.setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMntExpoyAsim(mntExpoyAsim);

		/*
		 * id:124 Total Monto Total: sin IVA es el de exportacion
		 */
		BigDecimal mntTotal = mntExpoyAsim;
		if (mntTotal.signum() < 0)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("MntTotal", mntTotal)
					.setDetailMessage("El monto total de la e-Factura de exportacion no puede ser negativo");
		totales.setMntTotal(mntTotal);

		/*
		 * id:129 Monto no facturable
		 */
		BigDecimal montoNF = noFacturable.subtract(noFacturableNegativo).setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMontoNF(montoNF);

		/*
		 * id:130 Monto total a pagar
		 */
		totales.setMntPagar(mntTotal.add(montoNF).setScale(2, BigDecimal.ROUND_HALF_UP));

		montosSinIva(mntExpoyAsim, mntTotal);
	}

	/**
	 * En el CFE: exportacion y total, los montos de IVA y retenciones en cero (los usan el PDF, el reporte diario y los
	 * listados)
	 */
	protected void montosSinIva(BigDecimal mntExpoyAsim, BigDecimal mntTotal) {
		strategy.getCFE().setTotMntExpyAsim(mntExpoyAsim.doubleValue());
		strategy.getCFE().setTotMntTotal(mntTotal.doubleValue());
		strategy.getCFE().setTotMntNoGrv(0d);
		strategy.getCFE().setTotMntImpPerc(0d);
		strategy.getCFE().setTotMntIVAenSusp(0d);
		strategy.getCFE().setTotMntIVATasaMin(0d);
		strategy.getCFE().setTotMntIVATasaBas(0d);
		strategy.getCFE().setTotMntIVAOtra(0d);
		strategy.getCFE().setMntIVATasaMin(0d);
		strategy.getCFE().setMntIVATasaBas(0d);
		strategy.getCFE().setMntIVAOtra(0d);
		strategy.getCFE().setIvaTasaMin(0d);
		strategy.getCFE().setIvaTasaBas(0d);
		strategy.getCFE().setTotMntRetenido(0d);
		strategy.getCFE().setTotValRetPerc(0d);
	}

	@Override
	public void buildComplementoFiscal(JSONObject complementoFiscalJson) throws APIException {
		if (complementoFiscalJson != null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CompFiscal")
					.setDetailMessage("La e-Factura de exportacion no lleva complemento fiscal (para exportacion como mandante usar ModVenta 91)");
	}

}
