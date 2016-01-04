package com.bluedot.efactura.test;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.concurrent.ThreadLocalRandom;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.EResg;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.soap.recepcion.Data;

public class PruebaTesting_GeneracionDocumentos {

	private static int DOC_QUANT = 50;
	private static int MAX_LINES = 15;

	public static void main(String[] args) {
		try {
			String encabezado = IO.readFile("resources/json/encabezado.json", Charset.defaultCharset());

			JSONObject encabezadoJSON = new JSONObject(encabezado);

			EFacturaFactory factory = new EFacturaFactoryImpl();

			RecepcionService service = new RecepcionServiceImpl();

			int[] resultFacturas = eFacturas(encabezadoJSON, factory, service);
			
			int[] resultTickets = eTickets(encabezadoJSON, factory, service);
			
			int[] resultResguardos = eResguardos(encabezadoJSON, factory, service);
			
			System.out.println("Tickets Correctos:" + resultTickets[0]);
			System.out.println("Tickets Credito Correctos:" + resultTickets[1]);
			System.out.println("Tickets Debito Correctos:" + resultTickets[2]);

			System.out.println("Facturas Correctos:" + resultFacturas[0]);
			System.out.println("Facturas Credito Correctos:" + resultFacturas[1]);
			System.out.println("Facturas Debito Correctos:" + resultFacturas[2]);
			
			System.out.println("Resguardos Correctos:" + resultResguardos[0]);
			
			
		} catch (JSONException | IOException | EFacturaException e) {
			e.printStackTrace();
		}

	}

	private static int[] eResguardos(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service) throws EFacturaException {
		/*
		 * eResguardo
		 */
		EResg[] eResguardos = new EResg[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			JSONArray detalleJSON = createRandomResguardoDetail();

			JSONObject resguardo = new JSONObject();
			resguardo.put("Encabezado", encabezadoJSON);
			resguardo.put("Detalle", detalleJSON);

			/*
			 * Create EResg object from json description
			 */
			EResg eResguardo = factory.getCFEController().createEResguardo(resguardo);
			eResguardos[i] = eResguardo;

		}
		
		int correctos = execute(service, eResguardos);
		
		int[] array = {correctos};
		
		return array;
		
	}

	

	

	private static int[] eFacturas(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service)
			throws EFacturaException {
		/*
		 * efactura
		 */
		EFact[] eFacturas = new EFact[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			JSONArray detalleJSON = createRandomDetail(null);

			JSONObject factura = new JSONObject();
			factura.put("Encabezado", encabezadoJSON);
			factura.put("Detalle", detalleJSON);

			/*
			 * Create Efact object from json description
			 */
			EFact eFactura = factory.getCFEController().createEfactura(factura);
			eFacturas[i] = eFactura;

		}

		/*
		 * Nota de credito de efactura
		 */
		EFact[] eFacturas_credito = new EFact[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			BigDecimal value = eFacturas[i].getDetalle().getItem().get(0).getMontoItem();
			JSONArray detalleJSON = createRandomDetail(value);

			JSONObject notaCredito = new JSONObject();
			notaCredito.put("Encabezado", encabezadoJSON);
			notaCredito.put("Detalle", detalleJSON);

			JSONObject referencia = getReferencia(eFacturas[i]);

			/*
			 * Create Nota de credito object from json description
			 */
			EFact eFactura = factory.getCFEController().createNotaCreditoEfactura(notaCredito, referencia);
			eFacturas_credito[i] = eFactura;

		}

		/*
		 * Nota de debito de efactura
		 */
		EFact[] eFacturas_debito = new EFact[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			BigDecimal value = eFacturas[i].getDetalle().getItem().get(0).getMontoItem();
			JSONArray detalleJSON = createRandomDetail(value);

			JSONObject notaDebito = new JSONObject();
			notaDebito.put("Encabezado", encabezadoJSON);
			notaDebito.put("Detalle", detalleJSON);

			JSONObject referencia = getReferencia(eFacturas[i]);
			/*
			 * Create Nota de debito object from json description
			 */
			EFact eFactura = factory.getCFEController().createNotaDebitoEfactura(notaDebito, referencia);
			eFacturas_debito[i] = eFactura;

		}

		/*
		 * Call the service
		 */
		int correctos = execute(service, eFacturas);

		int creditoCorrectos = execute(service, eFacturas_credito);

		int debitoCorrectos = execute(service, eFacturas_debito);
		
		int[] array = {correctos, creditoCorrectos, debitoCorrectos};
		
		return array;
	}
	
	private static int[] eTickets(JSONObject encabezadoJSON, EFacturaFactory factory, RecepcionService service)
			throws EFacturaException {
		/*
		 * efactura
		 */
		ETck[] eTickets = new ETck[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			JSONArray detalleJSON = createRandomDetail(null);

			JSONObject ticket = new JSONObject();
			ticket.put("Encabezado", encabezadoJSON);
			ticket.put("Detalle", detalleJSON);

			/*
			 * Create ETck object from json description
			 */
			ETck eTicket = factory.getCFEController().createETicket(ticket);
			eTickets[i] = eTicket;

		}

		/*
		 * Nota de credito de eTicket
		 */
		ETck[] eTickets_credito = new ETck[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			BigDecimal value = eTickets[i].getDetalle().getItem().get(0).getMontoItem();
			JSONArray detalleJSON = createRandomDetail(value);

			JSONObject notaCredito = new JSONObject();
			notaCredito.put("Encabezado", encabezadoJSON);
			notaCredito.put("Detalle", detalleJSON);

			JSONObject referencia = getReferencia(eTickets[i]);

			/*
			 * Create Nota de credito object from json description
			 */
			ETck eticket = factory.getCFEController().createNotaCreditoETicket(notaCredito, referencia);
			eTickets_credito[i] = eticket;

		}

		/*
		 * Nota de debito de eTicket
		 */
		ETck[] etickets_debito = new ETck[DOC_QUANT];
		for (int i = 0; i < DOC_QUANT; i++) {
			BigDecimal value = eTickets[i].getDetalle().getItem().get(0).getMontoItem();
			JSONArray detalleJSON = createRandomDetail(value);

			JSONObject notaDebito = new JSONObject();
			notaDebito.put("Encabezado", encabezadoJSON);
			notaDebito.put("Detalle", detalleJSON);

			JSONObject referencia = getReferencia(eTickets[i]);
			/*
			 * Create Nota de debito object from json description
			 */
			ETck eticket = factory.getCFEController().createNotaDebitoETicket(notaDebito, referencia);
			etickets_debito[i] = eticket;

		}

		/*
		 * Call the service
		 */
		int correctos = execute(service, eTickets);

		int creditoCorrectos = execute(service, eTickets_credito);

		int debitoCorrectos = execute(service, etickets_debito);
		
		int[] array = {correctos, creditoCorrectos, debitoCorrectos};
		
		return array;
	}

	private static int execute(RecepcionService service, ETck[] eTickets) throws EFacturaException {
		int correctos = 0; 
		for (int i = 0; i < eTickets.length; i++) {
			ETck eticket = eTickets[i];
			Data response;
			try {
				response = service.sendCFE(eticket);
				System.out.println("Output data:\n" + response.getXmlData());
				correctos++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return correctos;
	}
	
	private static int execute(RecepcionService service, EFact[] efacturas) throws EFacturaException {
		int correctos = 0; 
		for (int i = 0; i < efacturas.length; i++) {
			EFact eFactura = efacturas[i];
			Data response;
			try {
				response = service.sendCFE(eFactura);
				System.out.println("Output data:\n" + response.getXmlData());
				correctos++;
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		return correctos;
	}
	
	private static int execute(RecepcionService service, EResg[] eResguardos) throws EFacturaException {
		int correctos = 0; 
		for (int i = 0; i < eResguardos.length; i++) {
			EResg eResguardo = eResguardos[i];
			try {
				Data response = service.sendCFE(eResguardo);
				System.out.println("Output data:\n" + response.getXmlData());
				correctos++;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return correctos;
		
	}

	private static JSONObject getReferencia(ETck eTck) {
		JSONObject referencia = new JSONObject();
		SimpleDateFormat parserSDF=new SimpleDateFormat("yyyyMMdd");
		referencia.put("FechaCFEref", parserSDF.format(eTck.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		referencia.put("NroCFERef", String.valueOf(eTck.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eTck.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eTck.getEncabezado().getIdDoc().getTipoCFE()));
		return referencia;
	}

	

	private static JSONObject getReferencia(EFact eFact) {
		JSONObject referencia = new JSONObject();
		SimpleDateFormat parserSDF=new SimpleDateFormat("yyyyMMdd");
		referencia.put("FechaCFEref", parserSDF.format(eFact.getEncabezado().getIdDoc().getFchEmis().toGregorianCalendar().getTime()));
		referencia.put("NroCFERef", String.valueOf(eFact.getEncabezado().getIdDoc().getNro()));
		referencia.put("Serie", eFact.getEncabezado().getIdDoc().getSerie());
		referencia.put("TpoDocRef", String.valueOf(eFact.getEncabezado().getIdDoc().getTipoCFE()));
		return referencia;
	}

	private static JSONArray createRandomDetail(BigDecimal value) {

		JSONArray detalle = new JSONArray();

		if (value == null) {
			int cantLineas = ThreadLocalRandom.current().nextInt(1, MAX_LINES + 1);

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
	
	private static JSONArray createRandomResguardoDetail() {
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

	private static JSONObject createEntry(int i, int cant, int precioUnitario) {
		JSONObject linea = new JSONObject();
		linea.put("NroLinDet", i + 1);
		linea.put("IndFact", "3");
		linea.put("NomItem", "Item " + (i + 1));
		linea.put("Cantidad", String.valueOf(cant));
		linea.put("UniMed", "KG");
		linea.put("PrecioUnitario", String.valueOf(precioUnitario));
		linea.put("MontoItem", String.valueOf(precioUnitario * cant));
		return linea;
	}

}
