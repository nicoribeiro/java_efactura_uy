package com.bluedot.efactura.controllers;

import java.io.IOException;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.fasterxml.jackson.databind.JsonNode;

import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;

public class HomologacionController extends PruebasController {

	private Empresa empresa;
	
	public HomologacionController() {

		try {
			path = "resources/conf/cae_" + System.currentTimeMillis() + ".json";
			caeHomologacion = IO.readFile("resources/json/cae_homologacion.json", Charset.defaultCharset());
			caeActual = IO.readFile("resources/conf/cae.json", Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> generarPrueba() throws APIException {
		try {

			//TODO agregar control en los parametros que vienen
			JsonNode jsonNode = request().body().asJson();
			
			JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());
			
			String rut = encabezadoJSON.getJSONObject("Emisor").getString("RUCEmisor");
			
			empresa = Empresa.findByRUT(rut, true);

			JSONArray tiposDocArray = encabezadoJSON.getJSONArray("tiposDoc");
			
			detalle = IO.readFile(encabezadoJSON.getString("Detalle"), Charset.defaultCharset());

			loadTiposDoc(tiposDocArray);

			try {
				/*
				 * Guardo una copia del cae actual por seguridad
				 */
				IO.writeFile(path, caeActual);

				/*
				 * Uso el CAE con la numeracion de Homologacion
				 */
				switchToHomologacionCAE();

				JSONObject detalleJSON = new JSONObject(detalle);

				JSONArray resultFacturas = eFacturas(detalleJSON, encabezadoJSON);

				JSONArray resultTickets = eTickets(detalleJSON, encabezadoJSON);

				JSONObject result = new JSONObject();

				JSONArray aux = JSONUtils.concatArray(resultFacturas, resultTickets);

				result.put("resultado", aux);

				return json(result.toString());

			} catch (JSONException | IOException | APIException e) {
				throw APIException.raise(e);
			} finally {
				/*
				 * Uso el CAE con la numeracion de Homologacion
				 */
				switchToStandarCAE();
			}
		} catch (JSONException | IOException e) {
			throw APIException.raise(e);
		} 

	}

	private JSONArray eFacturas(JSONObject detalleJSON, JSONObject encabezadoJSON) throws APIException {

		/*
		 * Son + 1 porque repito el ultimo para anularlo
		 */
		CFE[] eFacturas = new CFE[detalleJSON.getJSONArray("111").length()+1];
		CFE[] eFacturas_credito = new CFE[detalleJSON.getJSONArray("112").length()+1];
		CFE[] eFacturas_debito = new CFE[detalleJSON.getJSONArray("113").length()+1];
		CFE[] eFacturas_contingencia = new CFE[detalleJSON.getJSONArray("211").length()];
		
		JSONObject result;
		JSONArray resultado = new JSONArray();

		if (tiposDoc.containsKey(TipoDoc.fromInt(111))) {
			/*
			 * efactura
			 */
			JSONObject factura=null;
			for (int i = 0; i < detalleJSON.getJSONArray("111").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("111").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				factura = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eFactura);
				encabezado.put("Receptor", receptor);
				factura.put("Encabezado", encabezado);

				factura.put("Detalle", detalle);

				/*
				 * Create Efact object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura, factura, true);
				eFacturas[i] = eFactura;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura, factura, true);
			eFacturas[detalleJSON.getJSONArray("111").length()] = eFactura;
		}
		
		result = execute(empresa, TipoDoc.eFactura, eFacturas, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(112))) {
			/*
			 * Nota de credito de efactura
			 */
			JSONObject notaCredito = null;
			JSONObject referencia = null;
			for (int i = 0; i < detalleJSON.getJSONArray("112").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("112").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				referencia = object.getJSONObject("Referencia");

				notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Credito_de_eFactura);
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);
				notaCredito.put("Referencia", referencia);
				notaCredito.put("Detalle", detalle);

				/*
				 * Create Nota de credito object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eFactura, notaCredito, true);
				eFacturas_credito[i] = eFactura;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eFactura, notaCredito, true);
			eFacturas_credito[detalleJSON.getJSONArray("112").length()] = eFactura;
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Credito_de_eFactura, eFacturas_credito, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(113))) {
			/*
			 * Nota de debito de efactura
			 */
			JSONObject notaDebito=null;
			JSONObject referencia = null;
			for (int i = 0; i < detalleJSON.getJSONArray("113").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("113").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				referencia = object.getJSONObject("Referencia");

				notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Debito_de_eFactura);
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);
				notaDebito.put("Referencia", referencia);
				notaDebito.put("Detalle", detalle);

				/*
				 * Create Nota de debito object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eFactura, notaDebito, true);
				eFacturas_debito[i] = eFactura;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eFactura, notaDebito, true);
			eFacturas_debito[detalleJSON.getJSONArray("113").length()] = eFactura;
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eFactura, eFacturas_debito, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(211))) {
			/*
			 * efactura contingencia
			 */
			factory.setModo(MODO_SISTEMA.CONTINGENCIA);

			for (int i = 0; i < detalleJSON.getJSONArray("211").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("211").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject efactura = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eFactura_Contingencia);
				encabezado.put("Receptor", receptor);
				efactura.put("Encabezado", encabezado);

				efactura.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura_Contingencia, efactura, true);
				eFacturas_contingencia[i] = eFactura;

			}
			factory.setModo(MODO_SISTEMA.NORMAL);
		}

		result = execute(empresa, TipoDoc.eFactura_Contingencia, eFacturas_contingencia, false);
		if (result != null)
			resultado.put(result);

		return resultado;
	}

	private JSONArray eTickets(JSONObject detalleJSON, JSONObject encabezadoJSON) throws APIException {

		/*
		 * Son + 1 porque repito el ultimo para anularlo
		 */
		CFE[] eTickets = new CFE[detalleJSON.getJSONArray("101").length()+1];
		CFE[] eTickets_credito = new CFE[detalleJSON.getJSONArray("102").length()+1];
		CFE[] etickets_debito = new CFE[detalleJSON.getJSONArray("103").length()+1];
		CFE[] eTicket_contingencia = new CFE[detalleJSON.getJSONArray("201").length()];

		JSONObject result;
		JSONArray resultado = new JSONArray();
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(101))) {
			/*
			 * eTickets
			 */
			JSONObject ticket=null;
			for (int i = 0; i < detalleJSON.getJSONArray("101").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("101").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				ticket = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eTicket);
				encabezado.put("Receptor", receptor);
				ticket.put("Encabezado", encabezado);

				ticket.put("Detalle", detalle);

				/*
				 * Create ETck object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket, ticket, true);
				eTickets[i] = eTicket;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket, ticket, true);
			eTickets[detalleJSON.getJSONArray("101").length()] = eTicket;
		}
		
		result = execute(empresa, TipoDoc.eTicket, eTickets, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(102))) {
			/*
			 * Nota de credito de eTicket
			 */
			JSONObject notaCredito = null;
			JSONObject referencia = null;
			for (int i = 0; i < detalleJSON.getJSONArray("102").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("102").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				referencia = object.getJSONObject("Referencia");

				notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Credito_de_eTicket);
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);
				notaCredito.put("Referencia", referencia);
				notaCredito.put("Detalle", detalle);

				/*
				 * Create Nota de credito object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eTicket, notaCredito, true);
				eTickets_credito[i] = eTicket;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eTicket,notaCredito, true);
			eTickets[detalleJSON.getJSONArray("102").length()] = eTicket;
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eTicket, eTickets_credito, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(103))) {
			/*
			 * Nota de debito de eTicket
			 */
			JSONObject notaDebito = null;
			JSONObject referencia = null;
			for (int i = 0; i < detalleJSON.getJSONArray("103").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("103").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				referencia = object.getJSONObject("Referencia");

				notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Debito_de_eTicket);
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);
				notaDebito.put("Referencia", referencia);
				notaDebito.put("Detalle", detalle);

				/*
				 * Create Nota de debito object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eTicket, notaDebito, true);
				etickets_debito[i] = eTicket;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eTicket,notaDebito, true);
			etickets_debito[detalleJSON.getJSONArray("103").length()] = eTicket;
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eTicket, etickets_debito, true);
		if (result != null)
			resultado.put(result);

		if (tiposDoc.containsKey(TipoDoc.fromInt(201))) {
			/*
			 * efactura contingencia
			 */
			factory.setModo(MODO_SISTEMA.CONTINGENCIA);

			for (int i = 0; i < detalleJSON.getJSONArray("201").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("201").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject ticket = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eTicket_Contingencia);
				encabezado.put("Receptor", receptor);
				ticket.put("Encabezado", encabezado);

				ticket.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket_Contingencia, ticket, true);
				eTicket_contingencia[i] = eTicket;

			}
			factory.setModo(MODO_SISTEMA.NORMAL);
		}

		result = execute(empresa, TipoDoc.eTicket_Contingencia, eTicket_contingencia, false);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

}
