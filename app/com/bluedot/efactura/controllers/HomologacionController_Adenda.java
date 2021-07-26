package com.bluedot.efactura.controllers;

import java.io.IOException;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.IO;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.Environment;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.asignarFecha.EstrategiaFechaAhora;
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
public class HomologacionController_Adenda extends PruebasController {

	private Empresa empresa;
	
	public HomologacionController_Adenda() {
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

				String caeHomologacion;
				
				caeHomologacion = IO.readFile("resources/json/cae_homologacion.json", Charset.defaultCharset());
				
				JSONObject data = new JSONObject(caeHomologacion);
				
				setHomologacionCAE(empresa, data.getJSONArray("data"));

				JSONObject detalleJSON = new JSONObject(detalle);

				JSONArray resultFacturas = eFacturas(detalleJSON, encabezadoJSON, output);

				JSONObject result = new JSONObject();

				result.put("resultado", resultFacturas);

				return json(result.toString());

			} catch (JSONException e) {
				throw APIException.raise(e);
			} 
		} catch (JSONException | IOException e) {
			throw APIException.raise(e);
		} 
	}

	private JSONArray eFacturas(JSONObject detalleJSON, JSONObject encabezadoJSON, String output) throws APIException {

	
		CFE[] eFacturas = new CFE[detalleJSON.getJSONArray("111").length()];
		
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
		}
		
		result = execute(empresa, TipoDoc.eFactura, eFacturas, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;
	}
}
