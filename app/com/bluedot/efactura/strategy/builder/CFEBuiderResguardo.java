package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.RetencionPercepcion;

import dgi.classes.recepcion.RetPercResg;
import dgi.classes.recepcion.TipMonType;
import dgi.classes.recepcion.TotalesResg.RetencPercep;
import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemResgWrapper;
import dgi.classes.recepcion.wrappers.RetPercInterface;
import dgi.classes.recepcion.wrappers.RetPercResgWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TotalesRetencPercepInterface;
import dgi.classes.recepcion.wrappers.TotalesRetencPercepResg;

public class CFEBuiderResguardo extends CFEBuilderImpl implements CFEBuiderInterface {

	
	public CFEBuiderResguardo(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		strategy.getCFE().setCantLineas(detalleJson.length());
		
		strategy.getCFE().setTotMntRetenido(new Double(0));
		
		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemResgWrapper item = (ItemResgWrapper) strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);

			item.setNroLinDet(i);

			if (itemJson.has("IndFact"))
				item.setIndFact(new BigInteger(Commons.safeGetString(itemJson,"IndFact")));

			JSONArray retencionesJSON = Commons.safeGetJSONArray(itemJson,"RetencPercep");

			if (retencionesJSON.length() > 5)
				throw APIException.raise(APIErrors.MALFORMED_CFE).setDetailMessage(
						"Se aceptan hasta 5 rentenciones por item, se enviaron " + retencionesJSON.length());
			
			List<RetPercInterface> retenciones = item.getRetencPerceps();
			
			for (int j = 0; j < retencionesJSON.length(); j++) {
				RetPercResg retencion = new RetPercResg();
				JSONObject retencionJSON = retencionesJSON.getJSONObject(j);
				RetencionPercepcion retencionPercepcion = new RetencionPercepcion();
				retencionPercepcion.save();
				
				if (retencionJSON.optString("CodRet") == null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("CodRet");
				retencion.setCodRet(retencionJSON.getString("CodRet"));
				retencionPercepcion.setCodigo(retencionJSON.getString("CodRet"));
				
				if (retencionJSON.has("Tasa") && retencionJSON.optString("Tasa")!= null){
					retencion.setTasa(new BigDecimal(retencionJSON.getString("Tasa")));
					retencionPercepcion.setTasa(Double.parseDouble(retencionJSON.getString("Tasa")));
				}
				
				if (retencionJSON.optString("MntSujetoaRet") == null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("MntSujetoaRet").setDetailMessage("MntSujetoaRet");
				retencion.setMntSujetoaRet(new BigDecimal(retencionJSON.getString("MntSujetoaRet")));
				retencionPercepcion.setMontoSujeto(Double.parseDouble(retencionJSON.getString("MntSujetoaRet")));
				
				if (retencionJSON.optString("ValRetPerc") == null)
					throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("ValRetPerc").setDetailMessage("ValRetPerc");
				retencion.setValRetPerc(new BigDecimal(retencionJSON.getString("ValRetPerc")));
				retencionPercepcion.setValor(Double.parseDouble(retencionJSON.getString("ValRetPerc")));
				
				strategy.getCFE().setTotMntRetenido(strategy.getCFE().getTotMntRetenido() + retencionPercepcion.getValor());
				
				strategy.getCFE().getRetencionesPercepciones().add(retencionPercepcion);

				retenciones.add(new RetPercResgWrapper(retencion));

			}
			
			item.setRetencPerceps(retenciones);

		}
	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException {
		TotalesInterface totales = strategy.getTotales();

		/*
		 * Moneda
		 */
		TipMonType moneda = TipMonType.fromValue(totalesJson.getString("TpoMoneda"));

		if (moneda == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TpoMoneda")
					.setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);
		strategy.getCFE().setMoneda(moneda);
		
		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio")){
				DecimalFormat df = new DecimalFormat("####0.000", DecimalFormatSymbols.getInstance(Locale.US));
				totales.setTpoCambio(new BigDecimal(df.format(totalesJson.getDouble("TpoCambio"))));
				strategy.getCFE().setTipoCambio(totales.getTpoCambio().doubleValue());
			}else
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("totales.TpoCambio");
		
		
		List<ItemInterface> items = strategy.getItem();
		
		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(items.size());
		
		BigDecimal total = new BigDecimal("0");
		
		List<TotalesRetencPercepInterface> totalesRetenciones = totales.getRetencPerceps();
		
		for (Iterator<ItemInterface> iterator = items.iterator(); iterator.hasNext();) {
			ItemInterface item = iterator.next();
			List<RetPercInterface> retencionesPercepciones = item.getRetencPerceps();
			
			TotalesRetencPercepResg retencion = new TotalesRetencPercepResg(new RetencPercep()); 
			
			for (Iterator<RetPercInterface> iterator2 = retencionesPercepciones.iterator(); iterator2.hasNext();) {
				RetPercInterface retPerc = iterator2.next();
				retencion.setCodRet(retPerc.getCodRet());
				retencion.setValRetPerc(retencion.getValRetPerc()!=null?retencion.getValRetPerc().add(retPerc.getValRetPerc()):retPerc.getValRetPerc());
				total = total.add(retPerc.getValRetPerc());
			}
			
			totalesRetenciones.add(retencion);
		}
		
		totales.setRetencPercep(totalesRetenciones);
		
		totales.setMntTotRetenido(total);
	}

}
