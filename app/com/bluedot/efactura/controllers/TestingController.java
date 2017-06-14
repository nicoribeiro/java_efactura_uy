package com.bluedot.efactura.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.TipoDoc;
import com.fasterxml.jackson.databind.JsonNode;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class TestingController extends PruebasController {

	private int cantidadDocumentos = 50;
	private int maxLineasPorDocumento = 15;

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> generarPrueba() throws APIException {

		JsonNode jsonNode = request().body().asJson();

		JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());

		JSONArray tiposDocArray = encabezadoJSON.getJSONArray("tiposDoc");

		cantidadDocumentos = encabezadoJSON.getInt("cantidadDocumentos");

		maxLineasPorDocumento = encabezadoJSON.getInt("maxLineasPorDocumento");

		loadTiposDoc(tiposDocArray);

		JSONArray resultFacturas = eFacturas(encabezadoJSON);

		JSONArray resultTickets = eTickets(encabezadoJSON);

		JSONArray resultResguardos = eResguardos(encabezadoJSON);

		JSONObject result = new JSONObject();

		JSONArray aux = concatArray(resultFacturas, resultTickets, resultResguardos);

		result.put("resultado", aux);

		return json(result.toString());

	}

	private JSONArray eResguardos(JSONObject encabezadoJSON)
			throws APIException {

		EResg[] eResguardos = new EResg[cantidadDocumentos];

		if (tiposDoc.containsKey(TipoDoc.fromInt(182))) {
			/*
			 * eResguardo
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomResguardoDetail();

				JSONObject resguardo = new JSONObject();
				resguardo.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				resguardo.put("Detalle", detalleJSON);

				/*
				 * Create EResg object from json description
				 */
				CFE eResguardo = factory.getCFEMicroController(empresa).create(TipoDoc.eResguardo, resguardo, true);
				eResguardos[i] = eResguardo.getEresguardo();

			}
		}
		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(TipoDoc.eResguardo, eResguardos, false);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eFacturas(JSONObject encabezadoJSON)
			throws APIException {

		EFact[] eFacturas = new EFact[cantidadDocumentos];
		EFact[] eFacturas_credito = new EFact[cantidadDocumentos];
		EFact[] eFacturas_debito = new EFact[cantidadDocumentos];

		if (tiposDoc.containsKey(TipoDoc.fromInt(111))) {
			/*
			 * efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomDetail(null);

				JSONObject factura = new JSONObject();
				factura.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				factura.put("Detalle", detalleJSON);

				/*
				 * Create Efact object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.eFactura,factura, true);
				eFacturas[i] = eFactura.getEfactura();

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(112))) {
			/*
			 * Nota de credito de efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaCredito.put("Detalle", detalleJSON);
				
				JSONObject referencia = getReferencia(eFacturas[i]);
				notaCredito.put("Referencia", referencia);
				/*
				 * Create Nota de credito object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eFactura, notaCredito, true);
				eFacturas_credito[i] = eFactura.getEfactura();

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(113))) {
			/*
			 * Nota de debito de efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaDebito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eFacturas[i]);
				notaDebito.put("Referencia", referencia);
				/*
				 * Create Nota de debito object from json description
				 */
				CFE eFactura = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eFactura, notaDebito, true);
				eFacturas_debito[i] = eFactura.getEfactura();

			}
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(TipoDoc.eFactura, eFacturas, false);
		if (result != null)
			resultado.put(result);

		result = execute( TipoDoc.Nota_de_Credito_de_eFactura, eFacturas_credito, false);
		if (result != null)
			resultado.put(result);

		result = execute( TipoDoc.Nota_de_Debito_de_eFactura, eFacturas_debito, false);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eTickets(JSONObject encabezadoJSON)
			throws APIException {

		ETck[] eTickets = new ETck[cantidadDocumentos];
		ETck[] eTickets_credito = new ETck[cantidadDocumentos];
		ETck[] etickets_debito = new ETck[cantidadDocumentos];

		if (tiposDoc.containsKey(TipoDoc.fromInt(101))) {
			/*
			 * efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				JSONArray detalleJSON = createRandomDetail(null);

				JSONObject ticket = new JSONObject();
				ticket.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				ticket.put("Detalle", detalleJSON);

				/*
				 * Create ETck object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.eTicket, ticket, true);
				eTickets[i] = eTicket.getEticket();

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(102))) {
			/*
			 * Nota de credito de eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaCredito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eTickets[i]);
				notaCredito.put("Referencia", referencia);
				/*
				 * Create Nota de credito object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Credito_de_eTicket,notaCredito, true);
				eTickets_credito[i] = eTicket.getEticket();

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(103))) {
			/*
			 * Nota de debito de eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getDetalle().getItems().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaDebito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eTickets[i]);
				notaDebito.put("Referencia", referencia);
				/*
				 * Create Nota de debito object from json description
				 */
				CFE eTicket = factory.getCFEMicroController(empresa).create(TipoDoc.Nota_de_Debito_de_eTicket,notaDebito, true);
				etickets_debito[i] = eTicket.getEticket();;

			}
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute( TipoDoc.eTicket, eTickets, false);
		if (result != null)
			resultado.put(result);

		result = execute( TipoDoc.Nota_de_Credito_de_eTicket, eTickets_credito, false);
		if (result != null)
			resultado.put(result);

		result = execute( TipoDoc.Nota_de_Debito_de_eTicket, etickets_debito, false);
		if (result != null)
			resultado.put(result);

		return resultado;
	}

	private JSONObject getReferencia(ETck eTck) {
		JSONObject referencia = new JSONObject();
		SimpleDateFormat parserSDF = new SimpleDateFormat("yyyyMMdd");
		referencia.put("FechaCFEref",
				parserSDF.format(eTck.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		referencia.put("NroCFERef", String.valueOf(eTck.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eTck.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eTck.getEncabezado().getIdDoc().getTipoCFE()));
		return referencia;
	}

	private JSONObject getReferencia(EFact eFact) {
		JSONObject referencia = new JSONObject();
		SimpleDateFormat parserSDF = new SimpleDateFormat("yyyyMMdd");
		referencia.put("FechaCFEref",
				parserSDF.format(eFact.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		referencia.put("NroCFERef", String.valueOf(eFact.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eFact.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eFact.getEncabezado().getIdDoc().getTipoCFE()));
		return referencia;
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
		item.put("Retenciones", retenciones);

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
