package com.bluedot.efactura.microControllers.implementation;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
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

	private CFE buildTemplate(JSONObject docJSON, CFEBuiderInterface cfeBuilder, JSONObject referencia)
			throws APIException {
		
		cfeBuilder.buildTimestampFirma();

		/*
		 * Encabezado
		 */
		JSONObject encabezadoJSON = Commons.safeGetJSONObject(docJSON,"Encabezado");

		int MntBruto = Commons.safeGetInteger(encabezadoJSON, "MntBruto");
		boolean montosIncluyenIva = (MntBruto==1); 
				
		int formaPago = Commons.safeGetInteger(encabezadoJSON, "FmaPago");
		
		/*
		 * Detalle (lineas de factura)
		 */
		cfeBuilder.buildDetalle(Commons.safeGetJSONArray(docJSON,"Detalle"), montosIncluyenIva);
		
		/*
		 * Emisor (dentro de encabezado)
		 */
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
		cfeBuilder.buildIdDoc(montosIncluyenIva, formaPago);
		
		/*
		 * Referencia
		 */
		cfeBuilder.buildReferencia(empresa, referencia);

		/*
		 * CAEData
		 */
		cfeBuilder.buildCAEData();
		
		/*
		 * JSON Generador
		 */
		cfeBuilder.getCFE().setGeneradorJson(docJSON.toString());
		
		/*
		 * ID Generador
		 */
		if (Commons.safeGetJSONObject(encabezadoJSON,"Identificacion").has("id"))
  			cfeBuilder.getCFE().setGeneradorId(Commons.safeGetJSONObject(encabezadoJSON,"Identificacion").getString("id"));
		
		return cfeBuilder.getCFE();
	}

	@Override
	public CFE create(TipoDoc tipo, JSONObject factura) throws APIException {
		return buildTemplate(factura, CFEBuilderFactory.getCFEBuilder(getTipoDoc(modo, tipo), caeMicroController), null);
	}


	@Override
	public CFE create(TipoDoc tipo, JSONObject jsonObject, JSONObject referencia) throws APIException {
		if (referencia == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Referencia"));

		return buildTemplate(jsonObject, CFEBuilderFactory.getCFEBuilder(getTipoDoc(modo, tipo), caeMicroController),
				referencia);

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
