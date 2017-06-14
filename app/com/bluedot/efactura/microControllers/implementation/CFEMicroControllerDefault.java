package com.bluedot.efactura.microControllers.implementation;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

public class CFEMicroControllerDefault extends MicroControllerDefault implements CFEMicroController {

	private MODO_SISTEMA modo;
	
	private CAEMicroController caeMicroController;
	
	public CFEMicroControllerDefault(MODO_SISTEMA modo, Empresa empresa, CAEMicroController caeMicroController) {
		super(empresa);
		this.modo = modo;
		this.caeMicroController = caeMicroController;
	}

	public CFE create(TipoDoc tipo, JSONObject docJSON, boolean esCfeEmitido) throws APIException {
		
		 CFEBuiderInterface cfeBuilder = CFEBuilderFactory.getCFEBuilder(getTipoDoc(modo, tipo), caeMicroController);
		
		/*
		 * Timestamp Firma
		 */
		cfeBuilder.buildTimestampFirma(docJSON.has("TmstFirma") ? docJSON.getLong("TmstFirma") : null);

		/*
		 * Adenda
		 */
		cfeBuilder.getCFE().setAdenda(docJSON.has("Adenda") ? docJSON.get("Adenda") instanceof JSONArray ? docJSON.getJSONArray("Adenda").toString(): docJSON.getString("Adenda") : null);
		
		/*
		 * Encabezado
		 */
		JSONObject encabezadoJSON = Commons.safeGetJSONObject(docJSON,"Encabezado");

		int MntBruto = Commons.safeGetInteger(Commons.safeGetJSONObject(encabezadoJSON, "IdDoc"), "MntBruto");
		boolean montosIncluyenIva = (MntBruto==1); 
				
		int formaPago = Commons.safeGetInteger(Commons.safeGetJSONObject(encabezadoJSON, "IdDoc"), "FmaPago");
		
		/*
		 * Detalle (lineas de factura)
		 */
		cfeBuilder.buildDetalle(Commons.safeGetJSONArray(docJSON,"Detalle"), montosIncluyenIva);
		
		/*
		 * Emisor (dentro de encabezado)
		 */
		if (encabezadoJSON.has("Emisor"))
			cfeBuilder.buildEmisor(Commons.safeGetJSONObject(encabezadoJSON, "Emisor"));
		else
			cfeBuilder.buildEmisor(empresa);
			
		
		/*
		 * Totales (dentro de encabezado)
		 */
		cfeBuilder.buildTotales(Commons.safeGetJSONObject(encabezadoJSON,"Totales"), montosIncluyenIva);

		/*
		 * Receptor (dentro de encabezado)
		 */
		cfeBuilder.buildReceptor(Commons.safeGetJSONObject(encabezadoJSON,"Receptor"));
		
		/*
		 * IdDoc (dentro de encabezado)
		 */
		cfeBuilder.buildIdDoc(montosIncluyenIva, formaPago, Commons.safeGetJSONObject(encabezadoJSON, "IdDoc"));
		
		/*
		 * Referencia
		 */
		cfeBuilder.buildReferencia(empresa, docJSON.has("Referencia") ?  Commons.safeGetJSONArray(docJSON,"Referencia") : null);

		/*
		 * CAEData
		 */
		if (esCfeEmitido)
			cfeBuilder.buildCAEData();
		
		/*
		 * JSON Generador
		 */
		cfeBuilder.getCFE().setGeneradorJson(docJSON.toString());
		
		/*
		 * ID Generador
		 */
		if (Commons.safeGetJSONObject(encabezadoJSON,"IdDoc").has("id"))
  			cfeBuilder.getCFE().setGeneradorId(Commons.safeGetJSONObject(encabezadoJSON,"IdDoc").getString("id"));
		
		return cfeBuilder.getCFE();
	}

	


	public static TipoDoc getTipoDoc(MODO_SISTEMA modo, TipoDoc tipo){
		if (modo==MODO_SISTEMA.NORMAL && tipo.value<200)
			return tipo;
		
		if (modo==MODO_SISTEMA.CONTINGENCIA && tipo.value>200)
			return tipo;
		
		if (modo==MODO_SISTEMA.NORMAL && tipo.value>200)
			return TipoDoc.fromInt(tipo.value-100);
		
		if (modo==MODO_SISTEMA.CONTINGENCIA && tipo.value<200)
			return TipoDoc.fromInt(tipo.value+100);
		
		return null;
	}

}
