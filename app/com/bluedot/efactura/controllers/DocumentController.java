package com.bluedot.efactura.controllers;

import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.Print;
import com.bluedot.efactura.GenerateInvoice;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.TipoDoc;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@Tx
@ErrorMessage
@Security.Authenticated(Secured.class)
public class DocumentController extends AbstractController {

	public Promise<Result> cambiarModo(String modo) throws APIException {
		MODO_SISTEMA modoEnum = MODO_SISTEMA.valueOf(modo);
		if (modoEnum==null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("modo"));
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		
		factory.setModo(modoEnum);
		
		return Promise.<Result> pure(ok());
	}
	
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> aceptarDocumento(String rut) throws APIException {
		//TODO meter mutex
		Empresa empresa = Empresa.findByRUT(rut, true);
		
		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();

		//TODO estos controles se pueden mover a una annotation
		if (!document.has("Encabezado") )
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Encabezado"));
		
		if (!document.getJSONObject("Encabezado").has("Identificacion"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Identificacion"));
		
		if (!document.getJSONObject("Encabezado").getJSONObject("Identificacion").has("tipo"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("tipo"));
		
		if (!document.getJSONObject("Encabezado").getJSONObject("Identificacion").has("id"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("id"));
		
		TipoDoc tipo = TipoDoc.fromInt(document.getJSONObject("Encabezado").getJSONObject("Identificacion").getInt("tipo"));

		if (tipo==null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("tipo"));
		
		String id = document.getJSONObject("Encabezado").getJSONObject("Identificacion").getString("id");
		
		CFE cfe = CFE.findByGeneradorId(empresa, id);
		
		if (cfe!=null)
			throw APIException.raise(APIErrors.EXISTE_CFE.withParams("generadorId", id));
		
		switch (tipo) {
		case eFactura:
		case eTicket:
		case eResguardo:
			cfe = factory.getCFEMicroController(empresa).create(tipo, document);
			factory.getServiceMicroController(empresa).register(cfe, null);
			break;
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eTicket:
			if (!document.getJSONObject("Encabezado").has("Referencia"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Referencia"));
			cfe = factory.getCFEMicroController(empresa).create(tipo, document, document.getJSONObject("Encabezado").getJSONObject("Referencia"));
			factory.getServiceMicroController(empresa).register(cfe, null);
			break;
			
		case eFactura_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eRemito:
		case eRemito_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
		case eResguardo_Contingencia:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case eTicket_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage(tipo.toString());
		}

		if (cfe!=null){
			JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);
		
			return json(cfeJson.toString());
		}else
			throw APIException.raise(APIErrors.NO_CFE_CREATED);
	}
	
//	public Promise<Result> anularDocumento(String rut, int idTipoDoc, String serie, int nro, String fecha) throws APIException {
//		
//		Empresa empresa = Empresa.findByRUT(rut, true);
//		
//		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
//		
//		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));
//		
//		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
//		
//		factory.getServiceMicroController(empresa).anularDocumento(tipo, serie, nro, date);
//		
//		return Promise.<Result> pure(ok());
//	}

	public Promise<Result> resultadoDocumentosFecha(String rut, String fecha) throws APIException {
		
		Empresa empresa = Empresa.findByRUT(rut, true);
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		factory.getServiceMicroController(empresa).consultarResultados(date);

		
		return Promise.<Result> pure(ok());

	}

	public Promise<Result> resultadoDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		
		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("TipoDoc", idTipoDoc));
		
		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);
		
		if (cfe.getEstado()==null)
			factory.getServiceMicroController(empresa).consultaResultado(cfe.getSobre());

		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

		return json(cfeJson.toString());
		
	}

	public Promise<Result> generarReporteDiario(String rut, String fecha, int cantReportes) throws APIException {

		if (cantReportes<1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("cantReportes", cantReportes));
		
		Empresa empresa = Empresa.findByRUT(rut, true);
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		
		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		List<ReporteDiario> reportes = new LinkedList<ReporteDiario>();
		
		for (int i = 0; i < cantReportes; i++) {
			ReporteDiario reporte = factory.getServiceMicroController(empresa).generarReporteDiario(DateHandler.add(date, i, Calendar.DAY_OF_MONTH), empresa);
			if (reporte!=null)
				reportes.add(reporte);
		}
		

		if (reportes.size()==cantReportes){
			JSONArray jsonResponse = EfacturaJSONSerializerProvider.getReporteDiarioSerializer().objectToJson(reportes);
			return json(jsonResponse.toString());
		}else
			throw APIException.raise(APIErrors.NO_REPORT_CREATED).setDetailMessage("Se solicitaron " + cantReportes + " reportes, pero se lograron crear " + reportes.size());
	}
	

	public Promise<Result> pdfDocumento(String rut, int nro, String serie, int idTipoDoc, boolean print) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);
		
		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);
		
		GenerateInvoice generateInvoice = new GenerateInvoice(cfe, new JSONObject(), empresa);

		try {
			File tempFile = File.createTempFile("345", ".pdf");
			
			ByteArrayOutputStream byteArrayOutputStream = generateInvoice.createPDF();
			
			try(OutputStream outputStream = new FileOutputStream(tempFile)) {
			    byteArrayOutputStream.writeTo(outputStream);
			}
			
			if (print)
			   Print.print(null, tempFile);
			
			return Promise.<Result> pure(ok(tempFile));
			
		} catch ( IOException | PrinterException e) {
			throw APIException.raise(e);
		} 
		
		
	}

}
