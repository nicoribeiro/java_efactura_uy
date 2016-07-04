package com.bluedot.efactura.controllers;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.DateHandler;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.EFacturaFactory.MODO;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.global.ErrorMessage;
import com.bluedot.efactura.global.Secured;
import com.bluedot.efactura.impl.CAEManagerImpl.TipoDoc;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.RecepcionService.ResultadoConsulta;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.classes.recepcion.CFEDefType.ETck;
import dgi.soap.recepcion.Data;
import flexjson.JSONSerializer;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class DocumentController extends Controller {

	public Result cambiarModo(String modo) throws EFacturaException {
		MODO modoEnum = MODO.valueOf(modo);
		if (modoEnum==null)
			throw EFacturaException.raise(EFacturaErrors.BAD_PARAMETER_VALUE).setDetailMessage("modo");
		
		EFacturaFactoryImpl.getInstance().setModo(modoEnum);
		
		return ok();
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public Result aceptarDocumento() throws EFacturaException {

		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());

		EFacturaFactory factory = EFacturaFactoryImpl.getInstance();

		RecepcionService service = new RecepcionServiceImpl();

		TipoDoc tipo = TipoDoc
				.fromInt(document.getInt("tipoDoc"));

		Data response = null;

		switch (tipo) {
		case Nota_de_Credito_de_eFactura:
			break;
		case Nota_de_Credito_de_eFactura_Contingencia:
			break;
		case Nota_de_Credito_de_eFactura_Exportacion:
			break;
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
			break;
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
			break;
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		case Nota_de_Credito_de_eTicket:
			break;
		case Nota_de_Credito_de_eTicket_Contingencia:
			break;
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
			break;
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		case Nota_de_Debito_de_eFactura:
			break;
		case Nota_de_Debito_de_eFactura_Contingencia:
			break;
		case Nota_de_Debito_de_eFactura_Exportacion:
			break;
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
			break;
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			break;
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		case Nota_de_Debito_de_eTicket:
			break;
		case Nota_de_Debito_de_eTicket_Contingencia:
			break;
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
			break;
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		case eFactura:
			EFact eFactura = factory.getCFEController().createEfactura(document);
			response = service.sendCFE(eFactura,null);
			break;
		case eFactura_Contingencia:
			break;
		case eFactura_Exportacion:
			break;
		case eFactura_Exportacion_Contingencia:
			break;
		case eFactura_Venta_por_Cuenta_Ajena:
			break;
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		case eRemito:
			break;
		case eRemito_Contingencia:
			break;
		case eRemito_de_Exportacion:
			break;
		case eRemito_de_Exportacion_Contingencia:
			break;
		case eResguardo:
			break;
		case eResguardo_Contingencia:
			break;
		case eTicket:
			ETck eticket = factory.getCFEController().createETicket(document);
			response = service.sendCFE(eticket,null);
			break;
		case eTicket_Contingencia:
			break;
		case eTicket_Venta_por_Cuenta_Ajena:
			break;
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			break;
		default:
			break;

		}

		JSONObject jsonResponse = new JSONObject();

		if (response != null) {
			jsonResponse.put("xmlResponse", response.getXmlData());
			return ok(jsonResponse.toString()).as("application/json");
		} else
			throw EFacturaException.raise(EFacturaErrors.INTERNAL_SERVER_ERROR);
	}
	
	public Result anularDocumento(int idTipoDoc, String serie, int nro, String fecha) throws EFacturaException {
		RecepcionService service = new RecepcionServiceImpl();
		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));
		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
		service.anularDocumento(tipo, serie, nro, date);
		return ok();
	}

	public Result resultadoDocumentosFecha(String fecha) throws EFacturaException {
		RecepcionService service = new RecepcionServiceImpl();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		List<ResultadoConsulta> result = service.consultarResultados(date);

		JSONArray array = new JSONArray();

		JSONSerializer serializer = new JSONSerializer().exclude("class").include("*");

		for (Iterator<ResultadoConsulta> iterator = result.iterator(); iterator.hasNext();) {
			ResultadoConsulta resultadoConsulta = iterator.next();

			array.put(new JSONObject(serializer.serialize(resultadoConsulta)));
		}

		return ok(array.toString()).as("application/json");

	}

	public Result resultadoDocumento(int nro, String serie, String fecha, int idTipoDoc) throws EFacturaException {
		RecepcionService service = new RecepcionServiceImpl();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		Data response = service.consultaResultado(tipo, serie, nro, date);

		JSONObject jsonResponse = new JSONObject();

		if (response != null) {
			jsonResponse.put("xmlResponse", response.getXmlData());
			return ok(jsonResponse.toString()).as("application/json");
		} else
			throw EFacturaException.raise(EFacturaErrors.INTERNAL_SERVER_ERROR);
	}

	public Result generarReporteDiario(String fecha) throws EFacturaException {
		RecepcionService service = new RecepcionServiceImpl();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		Data response = service.generarReporteDiario(date);

		JSONObject jsonResponse = new JSONObject();

		if (response != null) {
			jsonResponse.put("xmlResponse", response.getXmlData());
			return ok(jsonResponse.toString()).as("application/json");
		} else
			throw EFacturaException.raise(EFacturaErrors.INTERNAL_SERVER_ERROR);
	}

}
