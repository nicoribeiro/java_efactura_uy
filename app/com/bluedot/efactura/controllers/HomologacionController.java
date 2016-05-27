package com.bluedot.efactura.controllers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.EFacturaFactory.MODO;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.ETck;
import play.mvc.BodyParser;
import play.mvc.Result;

public class HomologacionController extends PruebasController {

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
	public Result generarPrueba() throws EFacturaException {
		try {

			JsonNode jsonNode = request().body().asJson();

			JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());

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

				EFacturaFactory factory = EFacturaFactoryImpl.getInstance();

				RecepcionService service = new RecepcionServiceImpl();

				JSONArray resultFacturas = eFacturas(detalleJSON, encabezadoJSON, factory, service);

				JSONArray resultTickets = eTickets(detalleJSON, encabezadoJSON, factory, service);

				JSONObject result = new JSONObject();

				JSONArray aux = concatArray(resultFacturas, resultTickets);

				result.put("resultado", aux);

				return ok(result.toString()).as("application/json");

			} catch (JSONException | IOException | EFacturaException e) {
				throw EFacturaException.raise(e);
			} finally {
				/*
				 * Uso el CAE con la numeracion de Homologacion
				 */
				switchToStandarCAE();
			}
		} catch (JSONException | IOException e) {
			throw EFacturaException.raise(e);
		} 

	}

	private JSONArray eFacturas(JSONObject detalleJSON, JSONObject encabezadoJSON, EFacturaFactory factory,
			RecepcionService service) throws EFacturaException {

		EFact[] eFacturas = new EFact[detalleJSON.getJSONArray("111").length()];
		EFact[] eFacturas_credito = new EFact[detalleJSON.getJSONArray("112").length()];
		EFact[] eFacturas_debito = new EFact[detalleJSON.getJSONArray("113").length()];
		EFact[] eFacturas_contingencia = new EFact[detalleJSON.getJSONArray("211").length()];

		if (tiposDoc.containsKey(TipoDoc.fromInt(111))) {
			/*
			 * efactura
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("111").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("111").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject factura = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				factura.put("Encabezado", encabezado);

				factura.put("Detalle", detalle);

				/*
				 * Create Efact object from json description
				 */
				EFact eFactura = factory.getCFEController().createEfactura(factura);
				eFacturas[i] = eFactura;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(112))) {
			/*
			 * Nota de credito de efactura
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("112").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("112").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				JSONObject referencia = object.getJSONObject("Referencia");

				JSONObject notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);

				notaCredito.put("Detalle", detalle);

				/*
				 * Create Nota de credito object from json description
				 */
				EFact eFactura = factory.getCFEController().createNotaCreditoEfactura(notaCredito, referencia);
				eFacturas_credito[i] = eFactura;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(113))) {
			/*
			 * Nota de debito de efactura
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("113").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("113").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				JSONObject referencia = object.getJSONObject("Referencia");

				JSONObject notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);

				notaDebito.put("Detalle", detalle);

				/*
				 * Create Nota de debito object from json description
				 */
				EFact eFactura = factory.getCFEController().createNotaDebitoEfactura(notaDebito, referencia);
				eFacturas_debito[i] = eFactura;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(211))) {
			/*
			 * efactura contingencia
			 */
			factory.setModo(MODO.CONTINGENCIA);

			for (int i = 0; i < detalleJSON.getJSONArray("211").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("211").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject efactura = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				efactura.put("Encabezado", encabezado);

				efactura.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				EFact eFactura = factory.getCFEController().createEfactura(efactura);
				eFacturas_contingencia[i] = eFactura;

			}
			factory.setModo(MODO.NORMAL);
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(service, 111, eFacturas);
		service.anularNextDocumento(TipoDoc.fromInt(111), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 112, eFacturas_credito);
		service.anularNextDocumento(TipoDoc.fromInt(112), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 113, eFacturas_debito);
		service.anularNextDocumento(TipoDoc.fromInt(113), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 211, eFacturas_contingencia);
		if (result != null)
			resultado.put(result);

		return resultado;
	}

	private JSONArray eTickets(JSONObject detalleJSON, JSONObject encabezadoJSON, EFacturaFactory factory,
			RecepcionService service) throws EFacturaException {

		ETck[] eTickets = new ETck[detalleJSON.getJSONArray("101").length()];
		ETck[] eTickets_credito = new ETck[detalleJSON.getJSONArray("102").length()];
		ETck[] etickets_debito = new ETck[detalleJSON.getJSONArray("103").length()];
		ETck[] eTicket_contingencia = new ETck[detalleJSON.getJSONArray("201").length()];

		if (tiposDoc.containsKey(TipoDoc.fromInt(101))) {
			/*
			 * eTickets
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("101").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("101").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject ticket = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				ticket.put("Encabezado", encabezado);

				ticket.put("Detalle", detalle);

				/*
				 * Create ETck object from json description
				 */
				ETck eTicket = factory.getCFEController().createETicket(ticket);
				eTickets[i] = eTicket;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(102))) {
			/*
			 * Nota de credito de eTicket
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("102").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("102").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				JSONObject referencia = object.getJSONObject("Referencia");

				JSONObject notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);

				notaCredito.put("Detalle", detalle);

				/*
				 * Create Nota de credito object from json description
				 */
				ETck eticket = factory.getCFEController().createNotaCreditoETicket(notaCredito, referencia);
				eTickets_credito[i] = eticket;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(103))) {
			/*
			 * Nota de debito de eTicket
			 */

			for (int i = 0; i < detalleJSON.getJSONArray("103").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("103").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");
				JSONObject referencia = object.getJSONObject("Referencia");

				JSONObject notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);

				notaDebito.put("Detalle", detalle);

				/*
				 * Create Nota de debito object from json description
				 */
				ETck eticket = factory.getCFEController().createNotaDebitoETicket(notaDebito, referencia);
				etickets_debito[i] = eticket;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(201))) {
			/*
			 * efactura contingencia
			 */
			factory.setModo(MODO.CONTINGENCIA);

			for (int i = 0; i < detalleJSON.getJSONArray("201").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("201").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONObject ticket = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object);
				encabezado.put("Receptor", receptor);
				ticket.put("Encabezado", encabezado);

				ticket.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				ETck eTicket = factory.getCFEController().createETicket(ticket);
				eTicket_contingencia[i] = eTicket;

			}
			factory.setModo(MODO.NORMAL);
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(service, 101, eTickets);
		service.anularNextDocumento(TipoDoc.fromInt(101), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 102, eTickets_credito);
		service.anularNextDocumento(TipoDoc.fromInt(102), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 103, etickets_debito);
		service.anularNextDocumento(TipoDoc.fromInt(103), new Date());
		if (result != null)
			resultado.put(result);

		result = execute(service, 201, eTicket_contingencia);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

}
