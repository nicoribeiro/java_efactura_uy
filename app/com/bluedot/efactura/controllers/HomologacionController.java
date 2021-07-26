package com.bluedot.efactura.controllers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.Environment;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.asignarFecha.EstrategiaFechaAhora;
import com.bluedot.efactura.strategy.numeracion.EstrategiaNumeracionOverride;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.Play;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class HomologacionController extends PruebasController {

	private Empresa empresa;
	
	public HomologacionController() {
		super();
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> generarPrueba() throws APIException {
		try {

			Environment env = Environment.valueOf(Play.application().configuration().getString(Constants.ENVIRONMENT));
			
			if (env==Environment.produccion) {
				throw APIException.raise(APIErrors.NO_SOPORTADO_EN_PRODUCCION);
			}
			
			
			//TODO agregar control en los parametros que vienen
			JsonNode jsonNode = request().body().asJson();
			
			JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());
			
			String rut = encabezadoJSON.getJSONObject("Emisor").getString("RUCEmisor");
			
			empresa = Empresa.findByRUT(rut, true);

			JSONArray tiposDocArray = encabezadoJSON.getJSONArray("tiposDoc");
			
			String detalle = IO.readFile(encabezadoJSON.getString("Detalle"), Charset.defaultCharset());

			String output = encabezadoJSON.has("Output")?encabezadoJSON.getString("Output"):"/tmp";
			
			loadTiposDoc(tiposDocArray);

			try {

				JSONArray caeHomologacion = generateCAE(tiposDocArray);
				
				setHomologacionCAE(empresa, caeHomologacion);

				JSONObject detalleJSON = new JSONObject(detalle);

				JSONArray resultFacturas = eFacturas(detalleJSON, encabezadoJSON, output);

				JSONArray resultTickets = eTickets(detalleJSON, encabezadoJSON, output);
				
				JSONArray resultResguardos = resguardos(detalleJSON, encabezadoJSON, output);

				JSONObject result = new JSONObject();

				JSONArray aux = JSONUtils.concatArray(resultFacturas, resultTickets, resultResguardos);

				result.put("resultado", aux);

				return json(result.toString());

			} catch (JSONException e) {
				throw APIException.raise(e);
			} 
		} catch (JSONException | IOException e) {
			throw APIException.raise(e);
		} 
	}

	private JSONArray generateCAE(JSONArray tiposDocArray) {
		JSONArray caes = new JSONArray();
		
		DateFormat df = new SimpleDateFormat("yy");
		String formattedDate = df.format(Calendar.getInstance().getTime());
		
		DateFormat df_long = new SimpleDateFormat("yyyy-MM-dd");
		DateFormat df_year = new SimpleDateFormat("yyyy");
		Calendar c = Calendar.getInstance();
		c.add(Calendar.YEAR, 1);
		c.set(Integer.parseInt(df_year.format(c.getTime())), 11, 31); 
		
		for (int i = 0; i < tiposDocArray.length(); i++) {
			JSONObject cae = new JSONObject();
			cae.put("FVD", df_long.format(c.getTime()));
			cae.put("TCFE", tiposDocArray.getInt(i));
			if (tiposDocArray.getInt(i)>200) {
				cae.put("HNro", 990000);
				cae.put("DNro", 951001);
				cae.put("Serie", "XA");
				cae.put("NA", "91" + formattedDate + "0002000");
			}else {
				cae.put("HNro", 100);
				cae.put("DNro", 1);
				cae.put("Serie", "A");
				cae.put("NA", "90" + formattedDate + "000" + tiposDocArray.get(i) + "0");
			}
			caes.put(cae);
		}
		
		return caes;
	}

	private JSONArray resguardos(JSONObject detalleJSON, JSONObject encabezadoJSON, String output) throws APIException {
		/*
		 * Son + 1 porque repito el ultimo para anularlo
		 */
		CFE[] eResguardos = new CFE[detalleJSON.getJSONArray("182").length()+1];
		CFE[] eResguardos_contingencia = new CFE[detalleJSON.getJSONArray("282").length()];
		
		JSONObject result;
		JSONArray resultado = new JSONArray();

		if (tiposDoc.containsKey(TipoDoc.fromInt(182))) {
			/*
			 * eResguardo
			 */
			JSONObject resguardo=null;
			for (int i = 0; i < detalleJSON.getJSONArray("182").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("182").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONArray referenciaArray = object.getJSONArray("Referencia");
				
				resguardo = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eResguardo, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				resguardo.put("Encabezado", encabezado);
				resguardo.put("Referencia", referenciaArray);
				resguardo.put("Detalle", detalle);

				/*
				 * Create CFE object from json description
				 */
				CFE eResguardo = factory.getCFEMicroController(empresa).create(TipoDoc.eResguardo, resguardo, true);
				eResguardos[i] = eResguardo;

			}
			
			/*
			 * Este es el CFE que se va a anular, lo que hace es repetir los datos del ultimo
			 */
			CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eResguardo, resguardo, true);
			eResguardos[detalleJSON.getJSONArray("182").length()] = eFactura;
		}
		
		result = execute(empresa, TipoDoc.eResguardo, eResguardos, true, output);
		if (result != null)
			resultado.put(result);
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(282))) {
			/*
			 * eResguardo contingencia
			 */
			factory.setModo(MODO_SISTEMA.CONTINGENCIA);

			for (int i = 0; i < detalleJSON.getJSONArray("282").length(); i++) {
				JSONObject object = detalleJSON.getJSONArray("282").getJSONObject(i);
				JSONArray detalle = object.getJSONArray("Detalle");

				JSONArray referenciaArray = object.getJSONArray("Referencia");
				
				JSONObject resguardo = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eResguardo_Contingencia, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				resguardo.put("Encabezado", encabezado);
				resguardo.put("Referencia", referenciaArray);
				resguardo.put("Detalle", detalle);

				/*
				 * Create eResguardo object from json description
				 */
				CFE eResguardo = factory.getCFEMicroController(empresa).create(TipoDoc.eResguardo_Contingencia, resguardo, true);
				int NroCFE = object.getInt("NroCFE");
				eResguardo.setEstrategiaNumeracion(new EstrategiaNumeracionOverride(NroCFE));
				eResguardos_contingencia[i] = eResguardo;

			}
			factory.setModo(MODO_SISTEMA.NORMAL);
		}

		result = execute(empresa, TipoDoc.eResguardo_Contingencia, eResguardos_contingencia, false, output);
		if (result != null)
			resultado.put(result);
		
		return resultado;
		
	}

		
	
	private JSONArray eFacturas(JSONObject detalleJSON, JSONObject encabezadoJSON, String output) throws APIException {

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
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eFactura, new EstrategiaFechaAhora());
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
		
		result = execute(empresa, TipoDoc.eFactura, eFacturas, true, output);
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
				referencia.put("NroLinRef", 1);
				JSONArray referenciaArray = new JSONArray();
				referenciaArray.put(referencia);
				
				notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Credito_de_eFactura, new EstrategiaFechaAhora());
				
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);
				notaCredito.put("Referencia", referenciaArray);
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
		
		result = execute(empresa, TipoDoc.Nota_de_Credito_de_eFactura, eFacturas_credito, true, output);
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
				referencia.put("NroLinRef", 1);
				JSONArray referenciaArray = new JSONArray();
				referenciaArray.put(referencia);
				
				notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Debito_de_eFactura, new EstrategiaFechaAhora());
				
				
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);
				notaDebito.put("Referencia", referenciaArray);
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
		
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eFactura, eFacturas_debito, true, output);
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
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eFactura_Contingencia, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				efactura.put("Encabezado", encabezado);

				efactura.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura_Contingencia, efactura, true);
				int NroCFE = object.getInt("NroCFE");
				eFactura.setEstrategiaNumeracion(new EstrategiaNumeracionOverride(NroCFE));
				eFacturas_contingencia[i] = eFactura;

			}
			factory.setModo(MODO_SISTEMA.NORMAL);
		}

		result = execute(empresa, TipoDoc.eFactura_Contingencia, eFacturas_contingencia, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;
	}

	private JSONArray eTickets(JSONObject detalleJSON, JSONObject encabezadoJSON, String output) throws APIException {

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
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eTicket, new EstrategiaFechaAhora());
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
		
		result = execute(empresa, TipoDoc.eTicket, eTickets, true, output);
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
				referencia.put("NroLinRef", 1);
				JSONArray referenciaArray = new JSONArray();
				referenciaArray.put(referencia);
				
				notaCredito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Credito_de_eTicket, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				notaCredito.put("Encabezado", encabezado);
				notaCredito.put("Referencia", referenciaArray);
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
			eTickets_credito[detalleJSON.getJSONArray("102").length()] = eTicket;
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Credito_de_eTicket, eTickets_credito, true, output);
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
				referencia.put("NroLinRef", 1);
				JSONArray referenciaArray = new JSONArray();
				referenciaArray.put(referencia);

				notaDebito = new JSONObject();
				JSONObject receptor = object.getJSONObject("Receptor");
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.Nota_de_Debito_de_eTicket, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				notaDebito.put("Encabezado", encabezado);
				notaDebito.put("Referencia", referenciaArray);
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
		
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eTicket, etickets_debito, true, output);
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
				JSONObject encabezado = getEncabezado(encabezadoJSON, object, TipoDoc.eTicket_Contingencia, new EstrategiaFechaAhora());
				encabezado.put("Receptor", receptor);
				ticket.put("Encabezado", encabezado);

				ticket.put("Detalle", detalle);

				/*
				 * Create Efactura object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket_Contingencia, ticket, true);
				int NroCFE = object.getInt("NroCFE");
				eTicket.setEstrategiaNumeracion(new EstrategiaNumeracionOverride(NroCFE));
				eTicket_contingencia[i] = eTicket;

			}
			factory.setModo(MODO_SISTEMA.NORMAL);
		}

		result = execute(empresa, TipoDoc.eTicket_Contingencia, eTicket_contingencia, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

}
