package com.bluedot.efactura.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.efactura.Constants;
import com.bluedot.efactura.Environment;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.strategy.asignarFecha.EstrategiaFechaRango;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.ETck;
import play.Play;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class TestingController extends PruebasController {

	private int cantidadDocumentos = 50;
	private int maxLineasPorDocumento = 15;
	private Empresa empresa;
	
	final static Logger logger = LoggerFactory.getLogger(TestingController.class);
	
	public static SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> generarPrueba() throws APIException {

		Environment env = Environment.valueOf(Play.application().configuration().getString(Constants.ENVIRONMENT));
		
		if (env==Environment.produccion) {
			throw APIException.raise(APIErrors.NO_SOPORTADO_EN_PRODUCCION);
		}
		
		JsonNode jsonNode = request().body().asJson();

		JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());

		String rut = encabezadoJSON.getJSONObject("Emisor").getString("RUCEmisor");
		
		empresa = Empresa.findByRUT(rut, true);
		
		JSONArray tiposDocArray = encabezadoJSON.getJSONArray("tiposDoc");

		cantidadDocumentos = encabezadoJSON.getInt("cantidadDocumentos");

		maxLineasPorDocumento = encabezadoJSON.getInt("maxLineasPorDocumento");
		
		String output = encabezadoJSON.has("Output")?encabezadoJSON.getString("Output"):"/tmp";

		loadTiposDoc(tiposDocArray);

		JSONArray resultFacturas = eFacturas(encabezadoJSON, output);

		JSONArray resultTickets = eTickets(encabezadoJSON, output);

		JSONArray resultResguardos = eResguardos(encabezadoJSON, output);

		JSONObject result = new JSONObject();

		JSONArray aux = JSONUtils.concatArray(resultFacturas, resultTickets, resultResguardos);

		result.put("resultado", aux);

		return json(result.toString());

	}

	private JSONArray eResguardos(JSONObject encabezadoJSON, String output)
			throws APIException {

		CFE[] eResguardos = new CFE[cantidadDocumentos];
		
		EstrategiaFechaRango estrategia = new EstrategiaFechaRango(5, 10, true);

		if (tiposDoc.containsKey(TipoDoc.fromInt(182))) {
			/*
			 * eResguardo
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomResguardoDetail();

				JSONObject resguardo = new JSONObject();
				resguardo.put("Encabezado", getEncabezado(encabezadoJSON, null, TipoDoc.eResguardo, estrategia));
				resguardo.put("Detalle", detalleJSON);

				JSONArray referencia = getReferencia();
				resguardo.put("Referencia", referencia);
				
				/*
				 * Create EResg object from json description
				 */
				logger.info(resguardo.toString());
				CFE eResguardo = factory.getCFEMicroController(empresa).create(TipoDoc.eResguardo, resguardo, true);
				eResguardos[i] = eResguardo;

			}
		}
		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(empresa, TipoDoc.eResguardo, eResguardos, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eFacturas(JSONObject encabezadoJSON, String output)
			throws APIException {

		CFE[] eFacturas = new CFE[cantidadDocumentos];
		CFE[] eFacturas_credito = new CFE[cantidadDocumentos];
		CFE[] eFacturas_debito = new CFE[cantidadDocumentos];

		JSONObject result;
		JSONArray resultado = new JSONArray();
		
		EstrategiaFechaRango estrategiaFacturas = new EstrategiaFechaRango(5, 10, true);
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(111))) {
			/*
			 * efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomDetail(null);

				JSONObject factura = new JSONObject();
				factura.put("Encabezado", getEncabezado(encabezadoJSON, null, TipoDoc.eFactura, estrategiaFacturas));
				factura.put("Detalle", detalleJSON);

				/*
				 * Create Efact object from json description
				 */
				logger.info(factura.toString());
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura,factura, true);
				eFacturas[i] = eFactura;

			}
		}
		
		/*
		 * Se envian las TipoDoc.eFactura
		 */
		result = execute(empresa, TipoDoc.eFactura, eFacturas, false, output);
		if (result != null)
			resultado.put(result);
		
		EstrategiaFechaRango estrategiaNotaCredito = new EstrategiaFechaRango(1, 4, true);

		if (tiposDoc.containsKey(TipoDoc.fromInt(112))) {
			/*
			 * Nota de credito de efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getEfactura().getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON, TipoDoc.Nota_de_Credito_de_eFactura, estrategiaNotaCredito));
				notaCredito.put("Detalle", detalleJSON);
				
				JSONArray referencia = getReferencia(eFacturas[i].getEfactura());
				notaCredito.put("Referencia", referencia);
				/*
				 * Create Nota de credito object from json description
				 */
				logger.info(notaCredito.toString());
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eFactura, notaCredito, true);
				eFacturas_credito[i] = eFactura;

			}
		}
		
		/*
		 * Se envian las TipoDoc.Nota_de_Credito_de_eFactura
		 */
		result = execute(empresa, TipoDoc.Nota_de_Credito_de_eFactura, eFacturas_credito, false, output);
		if (result != null)
			resultado.put(result);
		
		EstrategiaFechaRango estrategiaNotaDebito = new EstrategiaFechaRango(1, 4, true);

		if (tiposDoc.containsKey(TipoDoc.fromInt(113))) {
			/*
			 * Nota de debito de efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getEfactura().getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON, TipoDoc.Nota_de_Debito_de_eFactura, estrategiaNotaDebito));
				notaDebito.put("Detalle", detalleJSON);

				JSONArray referencia = getReferencia(eFacturas[i].getEfactura());
				notaDebito.put("Referencia", referencia);
				/*
				 * Create Nota de debito object from json description
				 */
				logger.info(notaDebito.toString());
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eFactura, notaDebito, true);
				eFacturas_debito[i] = eFactura;

			}
		}

		/*
		 * Se envian las TipoDoc.Nota_de_Debito_de_eFactura
		 */
		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eFactura, eFacturas_debito, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eTickets(JSONObject encabezadoJSON, String output)
			throws APIException {

		CFE[] eTickets = new CFE[cantidadDocumentos];
		CFE[] eTickets_credito = new CFE[cantidadDocumentos];
		CFE[] etickets_debito = new CFE[cantidadDocumentos];

		JSONObject result;
		JSONArray resultado = new JSONArray();
		
		EstrategiaFechaRango estrategiaTickets = new EstrategiaFechaRango(5, 10, true);
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(101))) {
			/*
			 * eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomDetail(null);

				JSONObject ticket = new JSONObject();
				ticket.put("Encabezado", getEncabezado(encabezadoJSON, null, TipoDoc.eTicket, estrategiaTickets));
				ticket.put("Detalle", detalleJSON);

				/*
				 * Create ETck object from json description
				 */
				logger.info(ticket.toString());
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket, ticket, true);
				eTickets[i] = eTicket;

			}
		}
		
		result = execute(empresa, TipoDoc.eTicket, eTickets, false, output);
		if (result != null)
			resultado.put(result);

		EstrategiaFechaRango estrategiaNotaCredito = new EstrategiaFechaRango(1, 4, true);
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(102))) {
			/*
			 * Nota de credito de eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getEticket().getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON, TipoDoc.Nota_de_Credito_de_eTicket, estrategiaNotaCredito));
				notaCredito.put("Detalle", detalleJSON);

				JSONArray referencia = getReferencia(eTickets[i].getEticket());
				notaCredito.put("Referencia", referencia);
				/*
				 * Create Nota de credito object from json description
				 */
				logger.info(notaCredito.toString());
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eTicket,notaCredito, true);
				eTickets_credito[i] = eTicket;

			}
		}
		
		result = execute(empresa, TipoDoc.Nota_de_Credito_de_eTicket, eTickets_credito, false, output);
		if (result != null)
			resultado.put(result);

		EstrategiaFechaRango estrategiaNotaDebito = new EstrategiaFechaRango(1, 4, true);
		
		if (tiposDoc.containsKey(TipoDoc.fromInt(103))) {
			/*
			 * Nota de debito de eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getEticket().getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON, TipoDoc.Nota_de_Debito_de_eTicket, estrategiaNotaDebito));
				notaDebito.put("Detalle", detalleJSON);

				JSONArray referencia = getReferencia(eTickets[i].getEticket());
				notaDebito.put("Referencia", referencia);
				/*
				 * Create Nota de debito object from json description
				 */
				logger.info(notaDebito.toString());
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eTicket,notaDebito, true);
				etickets_debito[i] = eTicket;

			}
		}

		result = execute(empresa, TipoDoc.Nota_de_Debito_de_eTicket, etickets_debito, false, output);
		if (result != null)
			resultado.put(result);

		return resultado;
	}

	private JSONArray getReferencia(ETck eTck) {
		JSONArray result = new JSONArray();
		JSONObject referencia = new JSONObject();
		referencia.put("NroCFERef", String.valueOf(eTck.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eTck.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eTck.getEncabezado().getIdDoc().getTipoCFE()));
		referencia.put("NroLinRef", 1);
		result.put(referencia);
		return result;
	}
	
	private JSONArray getReferencia() {
		JSONArray result = new JSONArray();
		JSONObject referencia = new JSONObject();
		referencia.put("NroCFERef", 23);
		referencia.put("Serie", "A");
		referencia.put("TpoDocRef", "111");
		referencia.put("NroLinRef", 1);
		result.put(referencia);
		return result;
	}
	

	private JSONArray getReferencia(EFact eFact) {
		JSONArray result = new JSONArray();
		JSONObject referencia = new JSONObject();
		referencia.put("NroCFERef", String.valueOf(eFact.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eFact.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eFact.getEncabezado().getIdDoc().getTipoCFE()));
		referencia.put("NroLinRef", 1);
		result.put(referencia);
		return result;
	}

	private JSONArray createRandomDetail(BigDecimal value) {

		JSONArray detalle = new JSONArray();

		if (value == null) {
			int cantLineas = ThreadLocalRandom.current().nextInt(1, maxLineasPorDocumento + 1);

			for (int i = 0; i < cantLineas; i++) {
				int cant = ThreadLocalRandom.current().nextInt(1, 5 + 1);
				int precioUnitario = ThreadLocalRandom.current().nextInt(1, 200 + 1);

				JSONObject linea = createEntry(i, cant, precioUnitario);
				detalle.put(linea);
			}
		} else {
			JSONObject linea = createEntry(1, 1, value.intValue());
			detalle.put(linea);
		}

		return detalle;
	}

	private JSONArray createRandomResguardoDetail() {
		JSONArray detalle = new JSONArray();

		JSONObject item = new JSONObject();
		JSONArray retenciones = new JSONArray();

		int cantLineas = ThreadLocalRandom.current().nextInt(1, 5 + 1);

		for (int i = 0; i < cantLineas; i++) {
			
			BigDecimal tasa = new BigDecimal("7");
			String CodRet = "1700082";
			BigDecimal MntSujetoaRet = new BigDecimal(String.valueOf(ThreadLocalRandom.current().nextInt(1, 200 + 1)));
			BigDecimal ValRetPerc = MntSujetoaRet.multiply(tasa.divide(new BigDecimal(100)));
			ValRetPerc.setScale(2, RoundingMode.HALF_DOWN);
			MntSujetoaRet.setScale(2, RoundingMode.HALF_DOWN);

			JSONObject linea = new JSONObject();
			linea.put("Tasa", String.valueOf(tasa));
			linea.put("CodRet", String.valueOf(CodRet));
			linea.put("MntSujetoaRet", String.valueOf(MntSujetoaRet));
			linea.put("ValRetPerc", String.valueOf(ValRetPerc));

			retenciones.put(linea);
		}
		item.put("RetencPercep", retenciones);
		item.put("NroLinDet", 1);
		
		detalle.put(item);

		return detalle;
	}

	private JSONObject createEntry(int i, int cant, int precioUnitario) {
		JSONObject linea = new JSONObject();
		linea.put("NroLinDet", i + 1);
		linea.put("IndFact", "3");
		linea.put("NomItem", "Produto de Prueba " + (i + 1));
		linea.put("Cantidad", String.valueOf(cant));
		linea.put("UniMed", "N/A");
		linea.put("PrecioUnitario", String.valueOf(precioUnitario));
		linea.put("MontoItem", String.valueOf(precioUnitario * cant));
		return linea;
	}

}
