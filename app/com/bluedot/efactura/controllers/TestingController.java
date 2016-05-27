package com.bluedot.efactura.controllers;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.ErrorMessage;
import com.bluedot.efactura.global.Secured;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class TestingController extends PruebasController {

	private int cantidadDocumentos = 50;
	private int maxLineasPorDocumento = 15;

	@BodyParser.Of(BodyParser.Json.class)
	public Result generarPrueba() throws EFacturaException {

		JsonNode jsonNode = request().body().asJson();

		JSONObject encabezadoJSON = new JSONObject(jsonNode.toString());

		JSONArray tiposDocArray = encabezadoJSON.getJSONArray("tiposDoc");

		cantidadDocumentos = encabezadoJSON.getInt("cantidadDocumentos");

		maxLineasPorDocumento = encabezadoJSON.getInt("maxLineasPorDocumento");

		loadTiposDoc(tiposDocArray);

		EFacturaFactory factory = EFacturaFactoryImpl.getInstance();

		RecepcionService service = new RecepcionServiceImpl();

		JSONArray resultFacturas = eFacturas(encabezadoJSON, factory, service);

		JSONArray resultTickets = eTickets(encabezadoJSON, factory, service);

		JSONArray resultResguardos = eResguardos(encabezadoJSON, factory, service);

		JSONObject result = new JSONObject();

		JSONArray aux = concatArray(resultFacturas, resultTickets, resultResguardos);

		result.put("resultado", aux);

		return ok(result.toString()).as("application/json");

	}

	private JSONArray eResguardos(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service)
			throws EFacturaException {

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
				EResg eResguardo = factory.getCFEController().createEResguardo(resguardo);
				eResguardos[i] = eResguardo;

			}
		}
		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(service, 182, eResguardos);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eFacturas(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service)
			throws EFacturaException {

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
				EFact eFactura = factory.getCFEController().createEfactura(factura);
				eFacturas[i] = eFactura;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(112))) {
			/*
			 * Nota de credito de efactura
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getDetalle().getItem().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaCredito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eFacturas[i]);

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
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eFacturas[i].getDetalle().getItem().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaDebito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eFacturas[i]);
				/*
				 * Create Nota de debito object from json description
				 */
				EFact eFactura = factory.getCFEController().createNotaDebitoEfactura(notaDebito, referencia);
				eFacturas_debito[i] = eFactura;

			}
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(service, 111, eFacturas);
		if (result != null)
			resultado.put(result);

		result = execute(service, 112, eFacturas_credito);
		if (result != null)
			resultado.put(result);

		result = execute(service, 113, eFacturas_debito);
		if (result != null)
			resultado.put(result);

		return resultado;

	}

	private JSONArray eTickets(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service)
			throws EFacturaException {

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
				ETck eTicket = factory.getCFEController().createETicket(ticket);
				eTickets[i] = eTicket;

			}
		}

		if (tiposDoc.containsKey(TipoDoc.fromInt(102))) {
			/*
			 * Nota de credito de eTicket
			 */
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getDetalle().getItem().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaCredito = new JSONObject();
				notaCredito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaCredito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eTickets[i]);

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
			for (int i = 0; i < cantidadDocumentos; i++) {
				BigDecimal value = eTickets[i].getDetalle().getItem().get(0).getMontoItem();
				JSONArray detalleJSON = createRandomDetail(value);

				JSONObject notaDebito = new JSONObject();
				notaDebito.put("Encabezado", getEncabezado(encabezadoJSON, encabezadoJSON));
				notaDebito.put("Detalle", detalleJSON);

				JSONObject referencia = getReferencia(eTickets[i]);
				/*
				 * Create Nota de debito object from json description
				 */
				ETck eticket = factory.getCFEController().createNotaDebitoETicket(notaDebito, referencia);
				etickets_debito[i] = eticket;

			}
		}

		/*
		 * Call the service
		 */
		JSONObject result;
		JSONArray resultado = new JSONArray();

		result = execute(service, 101, eTickets);
		if (result != null)
			resultado.put(result);

		result = execute(service, 102, eTickets_credito);
		if (result != null)
			resultado.put(result);

		result = execute(service, 103, etickets_debito);
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
