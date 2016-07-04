package com.bluedot.efactura.controllers;

import java.io.IOException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.impl.CAEManagerImpl;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.services.RecepcionService;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.soap.recepcion.Data;
import play.Logger;
import play.mvc.Controller;

public abstract class PruebasController extends Controller {

	protected String path;
	protected String detalle;
	protected String caeHomologacion;
	protected String caeActual;
	protected HashMap<TipoDoc, TipoDoc> tiposDoc = new HashMap<TipoDoc, TipoDoc>();

	public PruebasController() {
		super();
	}

	protected void switchToHomologacionCAE() {
		try {
			IO.writeFile("resources/conf/cae.json", caeHomologacion);
			CAEManagerImpl.getInstance().refreshMap();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EFacturaException e) {
			e.printStackTrace();
		}
	}

	protected void switchToStandarCAE() {
		try {
			IO.writeFile("resources/conf/cae.json", caeActual);
			CAEManagerImpl.getInstance().refreshMap();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (EFacturaException e) {
			e.printStackTrace();
		}
	}

	protected void loadTiposDoc(JSONArray tiposDocArray) {
		tiposDoc.clear();
		for (int i = 0; i < tiposDocArray.length(); i++) {
			TipoDoc tipoDoc = TipoDoc.fromInt(tiposDocArray.getInt(i));
			tiposDoc.put(tipoDoc, tipoDoc);
		}
	}

	protected JSONObject execute(RecepcionService service, int tipoDoc, EFact[] efacturas) throws EFacturaException {
		if (tiposDoc.containsKey(TipoDoc.fromInt(tipoDoc))) {
			int correctos = 0;
			for (int i = 0; i < efacturas.length; i++) {
				EFact eFactura = efacturas[i];
				Data response;
				try {
					response = service.sendCFE(eFactura,null);
					Logger.info("Output data:\n" + response.getXmlData());
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			Logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, efacturas.length, correctos);
		}
		return null;
	}

	protected JSONObject execute(RecepcionService service, int tipoDoc, EResg[] eResguardos) throws EFacturaException {
		if (tiposDoc.containsKey(TipoDoc.fromInt(tipoDoc))) {
			int correctos = 0;
			for (int i = 0; i < eResguardos.length; i++) {
				EResg eResguardo = eResguardos[i];
				try {
					Data response = service.sendCFE(eResguardo,null);
					Logger.info("Output data:\n" + response.getXmlData());
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, eResguardos.length, correctos);
		}
		return null;

	}

	protected JSONObject execute(RecepcionService service, int tipoDoc, ETck[] eTickets) throws EFacturaException {
		if (tiposDoc.containsKey(TipoDoc.fromInt(tipoDoc))) {
			int correctos = 0;
			for (int i = 0; i < eTickets.length; i++) {
				ETck eticket = eTickets[i];
				Data response;
				try {
					response = service.sendCFE(eticket,null);
					Logger.info("Output data:\n" + response.getXmlData());
					correctos++;
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			Logger.info(tipoDoc + " Correctos:" + correctos);
			return generarJSONResultado(tipoDoc, eTickets.length, correctos);
		}
		return null;
	}

	private JSONObject generarJSONResultado(int tipoDoc, int totalRegistros, int correctos) {
		JSONObject result = new JSONObject();
		JSONObject aux = new JSONObject();
		aux.put("totales", totalRegistros);
		aux.put("correctos", correctos);
		result.put(String.valueOf(tipoDoc), aux);
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