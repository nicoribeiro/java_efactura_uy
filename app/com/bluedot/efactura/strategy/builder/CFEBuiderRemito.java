package com.bluedot.efactura.strategy.builder;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.Detalle;

import dgi.classes.recepcion.wrappers.ItemInterface;
import dgi.classes.recepcion.wrappers.ItemRemWrapper;
import dgi.classes.recepcion.wrappers.TotalesInterface;
import dgi.classes.recepcion.wrappers.TpoCod;

public class CFEBuiderRemito extends CFEBuilderImpl implements CFEBuiderInterface {

	public CFEBuiderRemito(CAEMicroController caeMicroController, CFEStrategy strategy) throws APIException {
		super(caeMicroController, strategy);
	}

	@Override
	public void buildDetalle(JSONArray detalleJson, boolean montosIncluyenIva) throws APIException {

		for (int i = 1; i <= detalleJson.length(); i++) {
			ItemRemWrapper item = (ItemRemWrapper) strategy.createItem();
			JSONObject itemJson = detalleJson.getJSONObject(i - 1);

			item.setNroLinDet(i);

			if (itemJson.has("IndFact"))
				item.setIndFact(new BigInteger(Commons.safeGetString(itemJson,"IndFact")));

			
			item.setNomItem(Commons.safeGetString(itemJson,"NomItem"));

			item.setCantidad(new BigDecimal(Commons.safeGetString(itemJson,"Cantidad")));

			item.setUniMed(Commons.safeGetString(itemJson,"UniMed"));
			
			if (itemJson.has("DscItem") && !itemJson.getString("DscItem").equalsIgnoreCase("null") && !itemJson.getString("DscItem").equalsIgnoreCase(""))
				item.setDscItem(itemJson.getString("DscItem"));
 
			Detalle detalle = new Detalle(strategy.getCFE(), i,  item.getNomItem(), item.getCantidad().doubleValue(), item.getUniMed(), 0, 0);
			
			if (itemJson.has("DscItem"))
				detalle.setDescripcionItem(item.getDscItem());
			
			if (itemJson.has("CodItem")){
				String tpoCod = TpoCod.INT1.name();
				if (itemJson.has("TpoCod"))
					tpoCod = itemJson.getString("TpoCod");
				
				item.addCodItem(tpoCod, itemJson.getString("CodItem"));
				
				detalle.setCodItem(itemJson.getString("CodItem"));
				detalle.setTpoCod(tpoCod);
			}
			
			strategy.getCFE().getDetalle().add(detalle);

		}
	}

	@Override
	public void buildTotales(JSONObject totalesJson, boolean montosIncluyenIva) throws APIException {
		TotalesInterface totales = strategy.getTotales();

		List<ItemInterface> items = strategy.getItem();
		
		/*
		 * Cantidad de Lineas
		 */
		totales.setCantLinDet(items.size());
		
	}

	

}
