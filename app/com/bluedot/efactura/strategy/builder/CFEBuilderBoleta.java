package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.IndicadorFacturacion;

import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;

/**
 * Builder de la e-Boleta de entrada y sus notas (151-153, 251-253). Diferencias con la e-Factura (Formato CFE v25-2):
 * <ul>
 * <li>La emite el comprador: el receptor es el VENDEDOR (ver EBoletaStrategy.buildReceptor)</li>
 * <li>Items (B-C4): solo 13 (vendido por un no contribuyente), 14 (IVA minimo, Monotributo o Monotributo Social MIDES),
 * 15 (IMEBA), 12 (IVA en suspenso), 17 (compra de moneda extranjera para su reventa) y 5, 6 o 7. Con
 * IndCompraMEReventa (A-C21) solo 17; sin el, 17 no corresponde</li>
 * <li>Totales sin IVA (Totales_Boleta): MntNoGrv (A-C112) = suma de MontoItem con IndFact 13, 14, 15 o 17, MntIVaenSusp
 * (A-C115) = IndFact 12, MntTotal (A-C124) = MntNoGrv + MntIVaenSusp, MontoNF (A-C129) = IndFact 6 menos IndFact 7,
 * MntPagar (A-C130) = MntTotal + MontoNF</li>
 * <li>Sin retenciones/percepciones (IMEBA retenido, A-C125) ni complemento fiscal (IndCompraMEReventa 2): no se admiten
 * todavia</li>
 * </ul>
 */
public class CFEBuilderBoleta extends CFEBuilderImpl implements CFEBuiderInterface {

	private static final Set<String> IND_FACT = new HashSet<String>(Arrays.asList("5", "6", "7", "12", "13", "14", "15", "17"));

	public CFEBuilderBoleta(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}

	protected EBoletaStrategy getStrategy() {
		return (EBoletaStrategy) strategy;
	}

	private static String texto(JSONObject json, String campo) {
		if (!json.has(campo) || json.isNull(campo))
			return null;
		String valor = Commons.textoJson(json.get(campo)).trim();
		return valor.isEmpty() ? null : valor;
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		// Indicadores de facturacion admitidos en Item_Det_Boleta: antes de armar las lineas
		for (int i = 0; i < detalleJson.length(); i++) {
			JSONObject itemJson = detalleJson.getJSONObject(i);
			String indFact = texto(itemJson, "IndFact");
			if (indFact == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle[" + (i + 1) + "].IndFact");
			if (!IND_FACT.contains(indFact))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + (i + 1) + ": en la e-Boleta de entrada IndFact admite 13 (vendido por un no contribuyente), "
								+ "14 (IVA minimo o Monotributo), 15 (IMEBA), 12 (IVA en suspenso), 17 (compra de moneda extranjera para su reventa), "
								+ "5 (entrega gratuita), 6 (no facturable) o 7 (no facturable negativo)");
			if (itemJson.has("RetencPercep"))
				throw APIException.raise(APIErrors.NOT_SUPPORTED).withParams("RetencPercep")
						.setDetailMessage("Linea " + (i + 1) + ": la e-Boleta de entrada con retenciones/percepciones no se admite todavia");
		}

		super.buildDetalle(detalleJson, false);
	}

	@Override
	public void buildIdDoc(boolean montosIncluyenIva, Integer formaPago, JSONObject idDocJson) throws APIException {
		// MntBruto (A-C10) lo pone la estrategia: 2 si hay IVA en suspenso
		super.buildIdDoc(false, formaPago, idDocJson);

		/*
		 * A-C21: con compra de moneda extranjera para su reventa solo van items 17; sin ella, 17 no corresponde. El valor
		 * (1; 2 no se admite) lo valida DatosIdDoc
		 */
		boolean compraMEReventa = texto(idDocJson, "IndCompraMEReventa") != null;
		getStrategy().setCompraMEReventa(compraMEReventa);
		int ind17 = IndicadorFacturacion.INDICADOR_FACTURACION_COMPRA_ME_REVENTA.getIndice();
		for (ItemInterface item : strategy.getItem()) {
			int indFact = item.getIndFact().intValue();
			if (compraMEReventa && indFact != ind17)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + item.getNroLinDet() + ": con IndCompraMEReventa solo se admite IndFact 17");
			if (!compraMEReventa && indFact == ind17)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFact)
						.setDetailMessage("Linea " + item.getNroLinDet() + ": IndFact 17 es para la compra de moneda extranjera para su reventa (IndCompraMEReventa 1 en el IdDoc)");
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
		BigDecimal noGravado = BigDecimal.ZERO;
		BigDecimal ivaEnSuspenso = BigDecimal.ZERO;
		BigDecimal noFacturable = BigDecimal.ZERO;
		BigDecimal noFacturableNegativo = BigDecimal.ZERO;

		for (ItemInterface item : items) {
			int indFact = item.getIndFact().intValue();
			if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_NO_CONTRIBUYENTE.getIndice()
					|| indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_IVA_MINIMO.getIndice()
					|| indFact == IndicadorFacturacion.INDICADOR_FACTURACION_VENDIDO_POR_IMEBA.getIndice()
					|| indFact == IndicadorFacturacion.INDICADOR_FACTURACION_COMPRA_ME_REVENTA.getIndice())
				noGravado = noGravado.add(item.getMontoItem());
			else if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_IVA_SUSPENSO.getIndice())
				ivaEnSuspenso = ivaEnSuspenso.add(item.getMontoItem());
			else if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE.getIndice())
				noFacturable = noFacturable.add(item.getMontoItem());
			else if (indFact == IndicadorFacturacion.INDICADOR_FACTURACION_NO_FACTURABLE_NEGATIVO.getIndice())
				noFacturableNegativo = noFacturableNegativo.add(item.getMontoItem());
			// 5 (entrega gratuita): no suma
		}

		/*
		 * id:112 Total Monto - No Gravado (IndFact 13, 14, 15 y 17)
		 */
		BigDecimal mntNoGrv = noGravado.setScale(2, BigDecimal.ROUND_HALF_UP);
		if (mntNoGrv.signum() != 0)
			totales.setMntNoGrv(mntNoGrv);

		/*
		 * id:115 Total Monto - IVA en suspenso (IndFact 12)
		 */
		BigDecimal mntIVaenSusp = ivaEnSuspenso.setScale(2, BigDecimal.ROUND_HALF_UP);
		if (mntIVaenSusp.signum() != 0)
			totales.setMntIVaenSusp(mntIVaenSusp);

		/*
		 * id:124 Total Monto Total
		 */
		BigDecimal mntTotal = mntNoGrv.add(mntIVaenSusp);
		if (mntTotal.signum() < 0)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("MntTotal", mntTotal)
					.setDetailMessage("El monto total de la e-Boleta de entrada no puede ser negativo");
		totales.setMntTotal(mntTotal);

		/*
		 * id:129 Monto no facturable
		 */
		BigDecimal montoNF = noFacturable.subtract(noFacturableNegativo).setScale(2, BigDecimal.ROUND_HALF_UP);
		if (montoNF.signum() != 0)
			totales.setMontoNF(montoNF);

		/*
		 * id:130 Monto total a pagar
		 */
		totales.setMntPagar(mntTotal.add(montoNF).setScale(2, BigDecimal.ROUND_HALF_UP));

		/*
		 * En el CFE: los usan el PDF, el reporte diario, el tope de UI y los listados
		 */
		strategy.getCFE().setTotMntNoGrv(mntNoGrv.doubleValue());
		strategy.getCFE().setTotMntIVAenSusp(mntIVaenSusp.doubleValue());
		strategy.getCFE().setTotMntTotal(mntTotal.doubleValue());
		strategy.getCFE().setTotMntExpyAsim(0d);
		strategy.getCFE().setTotMntImpPerc(0d);
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
			throw APIException.raise(APIErrors.NOT_SUPPORTED).withParams("CompFiscal")
					.setDetailMessage("La e-Boleta de entrada con complemento fiscal (compra de moneda extranjera por cuenta ajena) no se admite todavia");
	}

}
