package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;

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

	protected JSONObject execute(Empresa empresa, TipoDoc tipoDoc, CFE[] cfes, boolean ultimoEsAnulado) throws APIException {
		if (tiposDoc.containsKey(tipoDoc)) {
			int correctos = 0;
			int anulados = 0;
			for (int i = 0; i < cfes.length; i++) {
				try {
					if (i==cfes.length-1 && ultimoEsAnulado){
						cfes[i].setEstado(EstadoACKCFEType.BE);
						anulados++;
					} else
						correctos++;
					cfes[i].save();
					factory.getServiceMicroController(empresa).enviar(cfes[i]);
					
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			logger.info(tipoDoc + " Correctos:" + correctos);
			logger.info(tipoDoc + " Anulados:" + anulados);
			return generarJSONResultado(tipoDoc, cfes.length, correctos, anulados);
		}
		return null;
	}


	private JSONObject generarJSONResultado(TipoDoc tipoDoc, int totalRegistros, int correctos, int anulados) {
		JSONObject result = new JSONObject();
		JSONObject aux = new JSONObject();
		aux.put("totales", totalRegistros);
		aux.put("correctos", correctos);
		aux.put("anulados", anulados);
		result.put(tipoDoc.name(), aux);
		return result;
	}

	protected JSONObject getEncabezado(JSONObject encabezadoJSON, JSONObject config, TipoDoc tipoDoc, Boolean shuffleDates) {
		JSONObject newEncabezado = new JSONObject(encabezadoJSON, JSONObject.getNames(encabezadoJSON));
		
		JSONObject idDoc = new JSONObject();
		
		idDoc.put("TipoCFE", tipoDoc.value);
		
		if (config!=null && config.has("MntBruto") && config.getInt("MntBruto") == 1) {
			idDoc.put("MntBruto", 1);
		}else
			idDoc.put("MntBruto", 0);
		
		idDoc.put("FmaPago", 2);
		
		Date fechaEmision;
		
		if (shuffleDates) {
			int randomNum = ThreadLocalRandom.current().nextInt(1, 10);
			fechaEmision = DateHandler.minus(new Date(), randomNum, Calendar.DAY_OF_MONTH);
		}
		else
			fechaEmision = new Date();
			
		idDoc.put("FchEmis", (new SimpleDateFormat("yyyy-MM-dd")).format(fechaEmision));
			
		newEncabezado.put("IdDoc", idDoc);
	
		if (config!=null && config.has("FmaPago")) {
			final int fmaPago = config.getInt("FmaPago");
			newEncabezado.put("FmaPago", fmaPago);
			idDoc.put("FmaPago", fmaPago);
		}
		
		return newEncabezado;
	}

}