package com.bluedot.efactura.microControllers.implementation;

import javax.inject.Inject;

import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroControllerFactory;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.builder.CFEBuilder;
import com.bluedot.efactura.strategy.builder.CFEBuilderProvider;
import com.google.inject.assistedinject.Assisted;

public class CFEMicroControllerDefault extends MicroControllerDefault implements CFEMicroController {

	private MODO_SISTEMA modo;
	
	private CFEBuilderProvider cfeBuilderProvider;
	
	private Commons commons;
	
	@Inject
	public CFEMicroControllerDefault(@Assisted MODO_SISTEMA modo, @Assisted Empresa empresa, Commons commons, CFEBuilderProvider cfeBuilderProvider) {
		super(empresa);
		this.modo = modo;
		this.cfeBuilderProvider = cfeBuilderProvider;
		this.commons = commons;
	}

	private CFE buildTemplate(JSONObject docJSON, CFEBuilder cfeBuilder, JSONObject referencia)
			throws APIException {
		
		cfeBuilder.buildTimestampFirma();

		/*
		 * Encabezado
		 */
		JSONObject encabezadoJSON = commons.safeGetJSONObject(docJSON,"Encabezado");

		int MntBruto = commons.safeGetInteger(encabezadoJSON, "MntBruto");
		boolean montosIncluyenIva = (MntBruto==1); 
				
		int formaPago = commons.safeGetInteger(encabezadoJSON, "FmaPago");
		
		/*
		 * Detalle (lineas de factura)
		 */
		cfeBuilder.buildDetalle(commons.safeGetJSONArray(docJSON,"Detalle"), montosIncluyenIva);
		
		/*
		 * Emisor (dentro de encabezado)
		 */
		cfeBuilder.buildEmisor(empresa);
		
		/*
		 * Totales (dentro de encabezado)
		 */
		cfeBuilder.buildTotales(commons.safeGetJSONObject(encabezadoJSON,"Totales"), montosIncluyenIva);

		/*
		 * Receptor (dentro de encabezado)
		 */
		cfeBuilder.buildReceptor(commons.safeGetJSONObject(encabezadoJSON,"Receptor"));
		
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
		if (commons.safeGetJSONObject(encabezadoJSON,"Identificacion").has("id"))
  			cfeBuilder.getCFE().setGeneradorId(commons.safeGetJSONObject(encabezadoJSON,"Identificacion").getString("id"));
		
		return cfeBuilder.getCFE();
	}

	@Override
	public CFE create(TipoDoc tipo, JSONObject factura) throws APIException {
		cfeBuilderProvider.setEmpresa(empresa);
		cfeBuilderProvider.setTipoDoc(tipo);
		
		return buildTemplate(factura, cfeBuilderProvider.get(), null);
	}


	@Override
	public CFE create(TipoDoc tipo, JSONObject jsonObject, JSONObject referencia) throws APIException {
		if (referencia == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Referencia"));

		cfeBuilderProvider.setEmpresa(empresa);
		cfeBuilderProvider.setTipoDoc(tipo);
		
		return buildTemplate(jsonObject, cfeBuilderProvider.get(),referencia);

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

	@Override
	public void setModo(MODO_SISTEMA modo) {
		this.modo = modo;
	}

}
