package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;

import dgi.classes.recepcion.CFEDefType.EResg;
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

	public CFEBuiderResguardo(EResg eResguardo, TipoDoc tipo) throws EFacturaException {
		this.strategy = (new CFEStrategy.Builder()).withEResg(eResguardo).withTipo(tipo).build();
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws EFacturaException {

		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemResgWrapper item = (ItemResgWrapper) strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);

			item.setNroLinDet(i);

			if (itemJson.optString("IndFact") == null)
				item.setIndFact(new BigInteger(itemJson.getString("IndFact")));

			if (itemJson.optJSONArray("Retenciones") == null)
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("Retenciones");
			JSONArray retencionesJSON = itemJson.getJSONArray("Retenciones");

			if (retencionesJSON.length() > 5)
				throw EFacturaException.raise(EFacturaErrors.MALFORMED_CFE).setDetailMessage(
						"Se aceptan hasta 5 rentenciones por item, se enviaron " + retencionesJSON.length());
			
			
			
			List<RetPercInterface> retenciones = item.getRetencPercep();
			
			for (int j = 0; j < retencionesJSON.length(); j++) {
				RetPercResg retencion = new RetPercResg();
				JSONObject retencionJSON = retencionesJSON.getJSONObject(j);
				
				
				if (retencionJSON.optString("CodRet") == null)
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("CodRet");
				retencion.setCodRet(retencionJSON.getString("CodRet"));

				if (retencionJSON.optString("Tasa") == null)
					retencion.setTasa(new BigDecimal(retencionJSON.getString("Tasa")));

				if (retencionJSON.optString("MntSujetoaRet") == null)
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("MntSujetoaRet");
				retencion.setMntSujetoaRet(new BigDecimal(retencionJSON.getString("MntSujetoaRet")));

				if (retencionJSON.optString("ValRetPerc") == null)
					throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER).setDetailMessage("ValRetPerc");
				retencion.setValRetPerc(new BigDecimal(retencionJSON.getString("ValRetPerc")));

				retenciones.add(new RetPercResgWrapper(retencion));

			}
			
			item.setRetencPercep(retenciones);

		}
	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws EFacturaException {
		TotalesInterface totales = strategy.getTotales();

		/*
		 * Moneda
		 */
		TipMonType moneda = TipMonType.fromValue(totalesJson.getString("TpoMoneda"));

		if (moneda == null)
			throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE)
					.setDetailMessage("El campo TpoMoneda no es ninguno de los conocidos, ver tabla de monedas.");

		totales.setTpoMoneda(moneda);

		/*
		 * Tipo de cambio
		 */
		if (moneda != TipMonType.UYU)
			if (totalesJson.has("TpoCambio"))
				totales.setTpoCambio(new BigDecimal(totalesJson.getString("TpoCambio")));
			else
				throw EFacturaException.raise(EFacturaErrors.MISSING_PARAMETER)
						.setDetailMessage("Falta el parametro totales.TpoCambio");
		
		
		List<ItemInterface> items = strategy.getItem();
		
		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(items.size());
		
		BigDecimal total = new BigDecimal("0");
		
		List<TotalesRetencPercepInterface> totalesRetenciones = totales.getRetencPercep();
		
		for (Iterator<ItemInterface> iterator = items.iterator(); iterator.hasNext();) {
			ItemInterface item = iterator.next();
			List<RetPercInterface> retencionesPercepciones = item.getRetencPercep();
			
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
