package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.Detalle;
import com.bluedot.efactura.model.Empresa;

import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TpoCod;

/**
 * Builder del e-Remito de Exportacion (124, 224). Como la e-Factura de exportacion (receptor del exterior, moneda y tipo
 * de cambio, sin IVA), con estas diferencias (Formato CFE v25-2):
 * <ul>
 * <li>IdDoc: sin forma de pago (A-C11) ni montos brutos (A-C10); TipoTraslado, ModVenta y ViaTransp obligatorios y
 * ClauVenta opcional (ver DatosIdDoc)</li>
 * <li>Items (Item_Rem_Exp): el indicador de facturacion (B-C4) es opcional y solo admite 5 (entrega gratuita: precio
 * unitario y monto en cero) y 8 (item a rebajar de otro e-Remito ya emitido: precio unitario &ge; 0, se indica en la
 * Referencia el e-Remito que ajusta). Sin indicador el precio unitario (B-C11) tiene que ser mayor que cero. No admite
 * descuentos ni recargos (B-C12 a B-C17); admite NCM (B-C7.1). MontoItem (B-C24) = Cantidad x PrecioUnitario</li>
 * <li>Totales (Totales_Rem_Exp): MntExpoyAsim (A-C113) = suma de MontoItem menos la de los items con IndFact 8,
 * MntTotal (A-C124) = MntExpoyAsim (puede ser negativo); no tiene MontoNF ni MntPagar</li>
 * <li>Referencia (zona F, condicional): obligatoria con items IndFact 8; si viene se arma aunque no haya</li>
 * </ul>
 */
public class CFEBuilderRemitoExportacion extends CFEBuilderExportacion {

	/** IndFact 5: entrega gratuita */
	static final int ENTREGA_GRATUITA = 5;
	/** IndFact 8: item a rebajar de otro e-Remito */
	static final int A_REBAJAR = 8;

	public CFEBuilderRemitoExportacion(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}

	@Override
	public void buildIdDoc(boolean montosIncluyenIva, Integer formaPago, JSONObject idDocJson) throws APIException {
		// El e-Remito de exportacion no tiene forma de pago (A-C11): si viene FmaPago se ignora
		super.buildIdDoc(false, null, idDocJson);
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		if (detalleJson.length() == 0)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle")
					.setDetailMessage("El e-Remito de exportacion tiene que tener al menos una linea");
		if (detalleJson.length() > 200)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Detalle", detalleJson.length())
					.setDetailMessage("El e-Remito de exportacion admite hasta 200 lineas");

		strategy.getCFE().setCantLineas(detalleJson.length());

		for (int i = 1; i <= detalleJson.length(); i++) {
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);
			String linea = "Linea " + i + ": ";

			/*
			 * Indicador de facturacion (B-C4): opcional, solo 5 u 8
			 */
			Integer indFact = null;
			String indFactTexto = texto(itemJson, "IndFact");
			if (indFactTexto != null) {
				try {
					indFact = Integer.parseInt(indFactTexto);
				} catch (NumberFormatException e) {
				}
				if (indFact == null || (indFact != ENTREGA_GRATUITA && indFact != A_REBAJAR))
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("IndFact", indFactTexto)
							.setDetailMessage(linea + "en el e-Remito de exportacion IndFact se omite o admite 5 (entrega gratuita) u 8 (item a rebajar de otro e-Remito)");
			}

			for (String campo : new String[] { "DescuentoPct", "DescuentoMonto", "RecargoPct", "RecargoMnt" })
				if (texto(itemJson, campo) != null)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo)
							.setDetailMessage(linea + "el e-Remito de exportacion no admite descuentos ni recargos por item");

			String nomItem = texto(itemJson, "NomItem");
			if (nomItem == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle[" + i + "].NomItem");
			String uniMed = texto(itemJson, "UniMed");
			if (uniMed == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle[" + i + "].UniMed")
						.setDetailMessage(linea + "falta la unidad de medida (UniMed, N/A si no corresponde)");
			String ncm = texto(itemJson, "NCM");
			if (ncm != null && ncm.length() > 20)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("NCM", ncm)
						.setDetailMessage(linea + "NCM admite hasta 20 caracteres");

			BigDecimal cantidad = numero(itemJson, "Cantidad", i);
			BigDecimal precioUnitario = numero(itemJson, "PrecioUnitario", i);

			/*
			 * Precio unitario (B-C11): > 0, salvo IndFact 5 (= 0) e IndFact 8 (>= 0)
			 */
			if (indFact != null && indFact == ENTREGA_GRATUITA) {
				if (precioUnitario.signum() != 0)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("PrecioUnitario", precioUnitario)
							.setDetailMessage(linea + "con IndFact 5 (entrega gratuita) el precio unitario tiene que ser 0");
			} else if (indFact != null && indFact == A_REBAJAR) {
				if (precioUnitario.signum() < 0)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("PrecioUnitario", precioUnitario)
							.setDetailMessage(linea + "con IndFact 8 el precio unitario no puede ser negativo");
			} else if (precioUnitario.signum() <= 0)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("PrecioUnitario", precioUnitario)
						.setDetailMessage(linea + "el precio unitario tiene que ser mayor que cero (para entregas gratuitas usar IndFact 5)");

			/*
			 * Monto (B-C24) = cantidad x precio unitario (sin descuentos ni recargos); cero con IndFact 5
			 */
			BigDecimal montoItem = texto(itemJson, "MontoItem") != null ? numero(itemJson, "MontoItem", i)
					: cantidad.multiply(precioUnitario).setScale(2, BigDecimal.ROUND_HALF_UP);
			if (indFact != null && indFact == ENTREGA_GRATUITA && montoItem.signum() != 0)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("MontoItem", montoItem)
						.setDetailMessage(linea + "con IndFact 5 (entrega gratuita) el monto tiene que ser 0");

			ItemInterface item = strategy.createItem();
			item.setNroLinDet(i);
			if (indFact != null)
				item.setIndFact(BigInteger.valueOf(indFact));
			item.setNomItem(nomItem);
			item.setCantidad(cantidad);
			item.setUniMed(uniMed);
			String dscItem = texto(itemJson, "DscItem");
			if (dscItem != null && !dscItem.equalsIgnoreCase("null"))
				item.setDscItem(dscItem);
			item.setPrecioUnitario(precioUnitario);
			item.setMontoItem(montoItem);

			Detalle detalle = new Detalle(strategy.getCFE(), i, nomItem, cantidad.doubleValue(), uniMed, precioUnitario.doubleValue(),
					montoItem.doubleValue());
			if (item.getDscItem() != null)
				detalle.setDescripcionItem(item.getDscItem());

			if (texto(itemJson, "CodItem") != null) {
				String tpoCod = texto(itemJson, "TpoCod") != null ? texto(itemJson, "TpoCod") : TpoCod.INT1.name();
				item.addCodItem(tpoCod, texto(itemJson, "CodItem"));
				detalle.setCodItem(texto(itemJson, "CodItem"));
				detalle.setTpoCod(tpoCod);
			}

			strategy.getCFE().getDetalle().add(detalle);
		}

		aplicarNCM(detalleJson);
	}

	private static BigDecimal numero(JSONObject itemJson, String campo, int linea) throws APIException {
		String valor = texto(itemJson, campo);
		if (valor == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Detalle[" + linea + "]." + campo);
		try {
			return new BigDecimal(valor);
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams(campo, valor)
					.setDetailMessage("Linea " + linea + ": " + campo + " tiene que ser numerico");
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
		 * id:113 (A-C113, si A-C2=124): suma de MontoItem menos la de los items a rebajar (IndFact 8). Los de entrega
		 * gratuita (IndFact 5) tienen monto 0
		 */
		BigDecimal mntExpoyAsim = BigDecimal.ZERO;
		for (ItemInterface item : items) {
			if (item.getIndFact() != null && item.getIndFact().intValue() == A_REBAJAR)
				mntExpoyAsim = mntExpoyAsim.subtract(item.getMontoItem());
			else
				mntExpoyAsim = mntExpoyAsim.add(item.getMontoItem());
		}
		mntExpoyAsim = mntExpoyAsim.setScale(2, BigDecimal.ROUND_HALF_UP);
		totales.setMntExpoyAsim(mntExpoyAsim);

		/*
		 * id:124 (A-C124): sin IVA es el de exportacion; en el e-Remito de exportacion puede ser negativo
		 */
		totales.setMntTotal(mntExpoyAsim);

		montosSinIva(mntExpoyAsim, mntExpoyAsim);
	}

	@Override
	protected boolean admiteReferencia() {
		return true;
	}

	@Override
	public void buildReferencia(Empresa empresaEmisora, JSONArray referenciasJSON) throws APIException {
		boolean hayARebajar = false;
		for (ItemInterface item : strategy.getItem())
			if (item.getIndFact() != null && item.getIndFact().intValue() == A_REBAJAR)
				hayARebajar = true;

		if (hayARebajar) {
			// B-C4=8: en el area de referencia se indica el e-Remito (de exportacion) que se ajusta
			if (referenciasJSON == null || referenciasJSON.length() == 0)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Referencia")
						.setDetailMessage("Con items a rebajar (IndFact 8) hay que referenciar el e-Remito de exportacion que se ajusta");
			boolean remitoReferenciado = false;
			for (int i = 0; i < referenciasJSON.length(); i++) {
				JSONObject referencia = referenciasJSON.getJSONObject(i);
				String tpoDocRef = texto(referencia, "TpoDocRef");
				if ("1".equals(texto(referencia, "IndGlobal")) || "124".equals(tpoDocRef) || "224".equals(tpoDocRef))
					remitoReferenciado = true;
			}
			if (!remitoReferenciado)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("Referencia")
						.setDetailMessage("Con items a rebajar (IndFact 8) la referencia tiene que ser un e-Remito de exportacion (TpoDocRef 124 o 224) o global (IndGlobal 1)");
		}

		super.buildReferencia(empresaEmisora, referenciasJSON);
	}

	@Override
	public void buildComplementoFiscal(JSONObject complementoFiscalJson) throws APIException {
		if (complementoFiscalJson != null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CompFiscal")
					.setDetailMessage("El e-Remito de exportacion no lleva complemento fiscal (para exportacion como mandante usar ModVenta 91)");
	}

}
