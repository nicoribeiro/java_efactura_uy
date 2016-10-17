package com.bluedot.efactura.controllers;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.classes.respuestas.cfe.EstadoACKCFEType;

public abstract class PruebasController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(PruebasController.class);
	
	protected String path;
	protected String detalle;
	protected String caeHomologacion;
	protected String caeActual;
	protected HashMap<TipoDoc, TipoDoc> tiposDoc = new HashMap<TipoDoc, TipoDoc>();
	protected EfacturaMicroControllersFactory factory;
	protected Empresa empresa;
	
	public PruebasController() {
		super();
		try {
			factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
			//TODO ver de inicializar esto bien
			empresa = new Empresa();
		} catch (APIException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void switchToHomologacionCAE() {
//		try {
			//TODO ver esto 
//			IO.writeFile("resources/conf/cae.json", caeHomologacion);
//			CAEManagerImpl.getInstance().refreshMap();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (APIException e) {
//			e.printStackTrace();
//		}
	}

	protected void switchToStandarCAE() {
		//TODO ver esto
//		try {
//			IO.writeFile("resources/conf/cae.json", caeActual);
//			CAEManagerImpl.getInstance().refreshMap();
//		} catch (IOException e) {
//			e.printStackTrace();
//		} catch (APIException e) {
//			e.printStackTrace();
//		}
	}

	protected void loadTiposDoc(JSONArray tiposDocArray) {
		tiposDoc.clear();
		for (int i = 0; i < tiposDocArray.length(); i++) {
			TipoDoc tipoDoc = TipoDoc.fromInt(tiposDocArray.getInt(i));
			tiposDoc.put(tipoDoc, tipoDoc);
		}
	}

	protected JSONObject execute(TipoDoc tipoDoc, EFact[] efacturas, boolean ultimoEsAnulado) throws APIException {
		if (tiposDoc.containsKey(tipoDoc)) {
			int correctos = 0;
			for (int i = 0; i < efacturas.length; i++) {
				EFact eFactura = efacturas[i];
				try {
					CFE cfe = new CFE();
					cfe.setTipo(tipoDoc);
					cfe.setEfactura(eFactura);
					if (i==efacturas.length-1 && ultimoEsAnulado){
						cfe.setEstado(EstadoACKCFEType.BE);
					}
					factory.getServiceMicroController(empresa).register(cfe, null);
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

	protected JSONObject execute(TipoDoc tipoDoc, EResg[] eResguardos, boolean ultimoEsAnulado) throws APIException {
		if (tiposDoc.containsKey(tipoDoc)) {
			int correctos = 0;
			for (int i = 0; i < eResguardos.length; i++) {
				EResg eResguardo = eResguardos[i];
				try {
					CFE cfe = new CFE();
					cfe.setTipo(tipoDoc);
					cfe.setEresguardo(eResguardo);
					if (i==eResguardos.length-1 && ultimoEsAnulado){
						cfe.setEstado(EstadoACKCFEType.BE);
					}
					factory.getServiceMicroController(empresa).register(cfe, null);
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, eResguardos.length, correctos);
		}
		return null;

	}

	protected JSONObject execute(TipoDoc tipoDoc, ETck[] eTickets, boolean ultimoEsAnulado) throws APIException {
		if (tiposDoc.containsKey(tipoDoc)) {
			int correctos = 0;
			for (int i = 0; i < eTickets.length; i++) {
				ETck eticket = eTickets[i];
				try {
					CFE cfe = new CFE();
					cfe.setTipo(tipoDoc);
					cfe.setEticket(eticket);
					if (i==eTickets.length-1 && ultimoEsAnulado){
						cfe.setEstado(EstadoACKCFEType.BE);
					}
					factory.getServiceMicroController(empresa).register(cfe, null);
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, eTickets.length, correctos);
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

	//TODO mover a un commons
	protected JSONArray concatArray(JSONArray... arrs) throws JSONException {
	    JSONArray result = new JSONArray();
	    for (JSONArray arr : arrs) {
	        for (int i = 0; i < arr.length(); i++) {
	            result.put(arr.get(i));
	        }
	    }
	    return result;
	}

	protected JSONObject getEncabezado(JSONObject encabezadoJSON, JSONObject object) {
		JSONObject newEncabezado = new JSONObject(encabezadoJSON, JSONObject.getNames(encabezadoJSON));
		if (object.has("MntBruto") && object.getInt("MntBruto") == 1) {
			newEncabezado.put("MntBruto", 1);
		}
	
		if (object.has("FmaPago")) {
			newEncabezado.put("FmaPago", object.getInt("FmaPago"));
		}
		
		if (object.has("Adenda")) {
			newEncabezado.put("Adenda", object.getString("Adenda"));
		}
	
		return newEncabezado;
	}

}