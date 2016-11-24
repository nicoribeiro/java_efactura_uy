package com.bluedot.efactura.controllers;

import java.awt.print.PrinterException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.inject.Inject;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.JSONUtils;
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

import play.Application;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

@Tx
@ErrorMessage
@Security.Authenticated(Secured.class)
public class DocumentController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(DocumentController.class);

	private Application application;
	
	@Inject
	public void setApplication(Application application) {
		this.application = application;
	}
	
	public CompletionStage<Result> cambiarModo(String modo) throws APIException {
		MODO_SISTEMA modoEnum = MODO_SISTEMA.valueOf(modo);
		if (modoEnum == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("modo"));

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		factory.setModo(modoEnum);

		return json(OK);
	}

	@BodyParser.Of(BodyParser.Json.class)
	public CompletionStage<Result> aceptarDocumento(String rut) throws APIException {
		// TODO meter mutex
		Empresa empresa = Empresa.findByRUT(rut, true);

		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		// TODO estos controles se pueden mover a una annotation
		if (!document.has("Encabezado"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Encabezado"));

		if (!document.getJSONObject("Encabezado").has("Identificacion"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Identificacion"));

		if (!document.getJSONObject("Encabezado").getJSONObject("Identificacion").has("tipo"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("tipo"));

		if (!document.getJSONObject("Encabezado").getJSONObject("Identificacion").has("id"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("id"));

		TipoDoc tipo = TipoDoc
				.fromInt(document.getJSONObject("Encabezado").getJSONObject("Identificacion").getInt("tipo"));

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("tipo"));

		String id = document.getJSONObject("Encabezado").getJSONObject("Identificacion").getString("id");

		CFE cfe = CFE.findByGeneradorId(empresa, id);

		if (cfe != null)
			throw APIException.raise(APIErrors.EXISTE_CFE.withParams("generadorId", id));

		switch (tipo) {
		case eFactura:
		case eTicket:
		case eResguardo:
			cfe = factory.getCFEMicroController(empresa).create(tipo, document);
			break;
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eTicket:
			if (!document.getJSONObject("Encabezado").has("Referencia"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER.withParams("Referencia"));
			cfe = factory.getCFEMicroController(empresa).create(tipo, document,
					document.getJSONObject("Encabezado").getJSONObject("Referencia"));
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

		if (cfe != null) {

			if (document.has("Adenda"))
				cfe.setAdenda(document.getJSONArray("Adenda").toString());

			JSONObject error = null;

			try {
				factory.getServiceMicroController(empresa).enviar(cfe);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}

			JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

			if (error != null)
				cfeJson = JSONUtils.merge(cfeJson, error);
			else
				cfeJson = JSONUtils.merge(cfeJson, new JSONObject(OK));

			try {
				generarPDF(empresa, cfe);
			} catch (Throwable e) {
			}

			return json(cfeJson.toString());
		} else
			throw APIException.raise(APIErrors.NO_CFE_CREATED);
	}

	@BodyParser.Of(BodyParser.Json.class)
	public CompletionStage<Result> reenviarDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {
		// TODO meter mutex
		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);

		JSONObject error = null;

		try {
			if (cfe.getEstado() == null)
				factory.getServiceMicroController(empresa).reenviar(cfe.getSobre());
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

		if (error != null)
			cfeJson = JSONUtils.merge(cfeJson, error);
		else
			cfeJson = JSONUtils.merge(cfeJson, new JSONObject(OK));

		try {
			generarPDF(empresa, cfe);
		} catch (Throwable e) {
		}

		return json(cfeJson.toString());
	}

	public CompletionStage<Result> anularDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);

		factory.getServiceMicroController(empresa).anularDocumento(cfe);

		return json(OK);
	}

	public CompletionStage<Result> resultadoDocumentosFecha(String rut, String fecha) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		factory.getServiceMicroController(empresa).consultarResultados(date);

		return json(OK);

	}

	public CompletionStage<Result> resultadoDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("TipoDoc", idTipoDoc));

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);

		JSONObject error = null;

		try {
			if (cfe.getEstado() == null)
				factory.getServiceMicroController(empresa).consultaResultado(cfe.getSobre());
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

		if (error != null)
			cfeJson = JSONUtils.merge(cfeJson, error);
		else
			cfeJson = JSONUtils.merge(cfeJson, new JSONObject(OK));

		return json(cfeJson.toString());

	}
	
	public CompletionStage<Result> enviarMailEmpresa(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("TipoDoc", idTipoDoc));

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);

		JSONObject error = null;

		try {
			factory.getServiceMicroController(empresa).enviarMailEmpresa(cfe);
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}

	public CompletionStage<Result> procesarEmailEntrantes(String rut) throws APIException {
		Empresa empresaReceptora = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		JSONObject error = null;

		try {
				factory.getServiceMicroController(empresaReceptora).getDocumentosEntrantes();
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}
		
		return json(OK);

	}
	
	public CompletionStage<Result> getDocumentosEntrantes(String rut, String fecha) throws APIException {
		Empresa empresaReceptora = Empresa.findByRUT(rut, true);
		
		//TODO serializar los Sobres_recibidos y devolver
		return json(OK);

	}

	public CompletionStage<Result> generarReporteDiario(String rut, String fecha, int cantReportes) throws APIException {

		if (cantReportes < 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE.withParams("cantReportes", cantReportes));

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		JSONArray reportes = new JSONArray();

		for (int i = 0; i < cantReportes; i++) {
			JSONObject error = null;
			ReporteDiario reporte = null;
			try {

				reporte = factory.getServiceMicroController(empresa)
						.generarReporteDiario(DateHandler.add(date, i, Calendar.DAY_OF_MONTH));

			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}

			JSONObject reporteJSON = new JSONObject();

			if (reporte != null)
				reporteJSON = EfacturaJSONSerializerProvider.getReporteDiarioSerializer().objectToJson(reporte);

			if (error != null)
				reportes.put(JSONUtils.merge(reporteJSON, error));
			else
				reportes.put(JSONUtils.merge(reporteJSON, new JSONObject(OK)));

		}

		return json(reportes.toString());
	}

	public CompletionStage<Result> pdfDocumento(String rut, int nro, String serie, int idTipoDoc, boolean print)
			throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);

		try {
			File pdf = generarPDF(empresa, cfe);

			if (print)
				Print.print(null, pdf);

			return CompletableFuture.completedFuture(ok(pdf));

		} catch (IOException | PrinterException e) {
			throw APIException.raise(e);
		}

	}

	/**
	 * @param nro
	 * @param serie
	 * @param idTipoDoc
	 * @param empresa
	 * @param cfe
	 * @return
	 * @throws IOException
	 * @throws FileNotFoundException
	 */
	private File generarPDF(Empresa empresa, CFE cfe) throws IOException, FileNotFoundException {

		logger.info("Generando PDF tipoDoc:{} - serie:{} - nro:{}", cfe.getTipo().value, cfe.getSerie(), cfe.getNro());

		GenerateInvoice generateInvoice = new GenerateInvoice(cfe, empresa);

		String filename = cfe.getTipo().value + "-" + cfe.getSerie() + "-" + cfe.getNro() + ".pdf";

		String path = application.configuration().getString("documentos.pdf.path", "/mnt/efacturas");

		File pdf = new File(path + File.separator + filename);

		ByteArrayOutputStream byteArrayOutputStream = generateInvoice.createPDF();

		try (OutputStream outputStream = new FileOutputStream(pdf)) {
			byteArrayOutputStream.writeTo(outputStream);
		}
		return pdf;
	}

	// public CompletionStage<Result> printDocumento(String rut, int nro, String serie,
	// int idTipoDoc, boolean print)
	// throws APIException {
	//
	// Empresa empresa = Empresa.findByRUT(rut, true);
	//
	// TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
	//
	// CFE cfe = CFE.findById(empresa, tipo, serie, nro, true);
	//
	// GenerateInvoice generateInvoice = new GenerateInvoice(cfe, new
	// JSONObject(), empresa);
	//
	// try {
	// File tempFile = File.createTempFile("345", ".pdf");
	//
	// ByteArrayOutputStream byteArrayOutputStream =
	// generateInvoice.createPDF();
	//
	// try (OutputStream outputStream = new FileOutputStream(tempFile)) {
	// byteArrayOutputStream.writeTo(outputStream);
	// }
	//
	//// https://pdfbox.apache.org/index.html
	// http://stackoverflow.com/questions/29755305/itext-direct-printing
	// http://stackoverflow.com/questions/18636622/pdfbox-how-to-print-pdf-with-specified-printer
	//// byte[] pdfbyte = byteArrayOutputStream.toByteArray();
	//// //System.out.println(pdf);
	//// InputStream bis = new ByteArrayInputStream(pdfbyte);
	//// SimpleDoc pdfp = new SimpleDoc(bis, DocFlavor.BYTE_ARRAY.AUTOSENSE,
	// null);
	//// DocPrintJob printjob= printService.createPrintJob();
	//// printjob.print(pdfp, new HashPrintRequestAttributeSet());
	//// bis.close();
	//
	// return CompletableFuture.completedFuture(ok());
	//
	// } catch (IOException | PrinterException e) {
	// throw APIException.raise(e);
	// }
	//
	// }
	//
	// private void print(){
	//
	// }

}
