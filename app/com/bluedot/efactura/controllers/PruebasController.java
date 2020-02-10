package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;

public abstract class PruebasController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(PruebasController.class);
	
	protected HashMap<TipoDoc, TipoDoc> tiposDoc = new HashMap<TipoDoc, TipoDoc>();
	protected EfacturaMicroControllersFactory factory;
	
	public PruebasController() {
		super();
		try {
			factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		} catch (APIException e) {
			e.printStackTrace();
		}
	}

	protected void setHomologacionCAE(Empresa empresa, JSONArray caes) throws APIException {
		
		CAEMicroController caeMicroController = factory.getCAEMicroController(empresa);
		
		for (int i = 0; i < caes.length(); i++) {
			
			CAE cae = caeMicroController.getCAEfromJson(caes.getJSONObject(i));
			
			caeMicroController.anularCAEs(cae.getTipo());
			
			caeMicroController.addCAE(cae);
		}
	}

	protected void loadTiposDoc(JSONArray tiposDocArray) {
		tiposDoc.clear();
		for (int i = 0; i < tiposDocArray.length(); i++) {
			TipoDoc tipoDoc = TipoDoc.fromInt(tiposDocArray.getInt(i));
			tiposDoc.put(tipoDoc, tipoDoc);
		}
	}

	protected JSONObject execute(Empresa empresa, TipoDoc tipoDoc, CFE[] efacturas, boolean ultimoEsAnulado) throws APIException {
		if (tiposDoc.containsKey(tipoDoc)) {
			int correctos = 0;
			for (int i = 0; i < efacturas.length; i++) {
				try {
					if (i==efacturas.length-1 && ultimoEsAnulado){
						efacturas[i].setEstado(EstadoACKCFEType.BE);
					}
					factory.getServiceMicroController(empresa).enviar(efacturas[i]);
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, efacturas.length, correctos);
		}
		return null;
	}


	private JSONObject generarJSONResultado(TipoDoc tipoDoc, int totalRegistros, int correctos) {
		JSONObject result = new JSONObject();
		JSONObject aux = new JSONObject();
		aux.put("totales", totalRegistros);
		aux.put("correctos", correctos);
		result.put(tipoDoc.name(), aux);
		return result;
	}

	protected JSONObject getEncabezado(JSONObject encabezadoJSON, JSONObject config, TipoDoc tipoDoc) {
		JSONObject newEncabezado = new JSONObject(encabezadoJSON, JSONObject.getNames(encabezadoJSON));
		
		JSONObject idDoc = new JSONObject();
		
		idDoc.put("TipoCFE", tipoDoc.value);
		
		if (config!=null && config.has("MntBruto") && config.getInt("MntBruto") == 1) {
			idDoc.put("MntBruto", 1);
		}else
			idDoc.put("MntBruto", 0);
		
		idDoc.put("FmaPago", 2);
		
		idDoc.put("FchEmis", DateHandler.nowDate(new SimpleDateFormat("yyyy-MM-dd")));
		
		newEncabezado.put("IdDoc", idDoc);
	
		if (config!=null && config.has("FmaPago")) {
			newEncabezado.put("FmaPago", config.getInt("FmaPago"));
		}
		
		return newEncabezado;
	}

}