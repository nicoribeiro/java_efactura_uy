package com.bluedot.efactura.controllers;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.utils.DateHandler;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.Print;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.GenerateInvoice;
import com.bluedot.efactura.MODO_SISTEMA;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.*;
import com.bluedot.efactura.pollers.PollerManager;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;
import play.mvc.Security;

import java.awt.print.PrinterException;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@Api(value = "Operaciones de Documentos") 
public class DocumentController extends AbstractController {

	private static boolean initialized = false;
	
	final static Logger logger = LoggerFactory.getLogger(DocumentController.class);

	private PollerManager pollerManager;
	
	@Inject
	public DocumentController(PollerManager pollerManager){
		super();
		this.pollerManager = pollerManager;
		if (!initialized) {
			init();
		}
	}
	
	private synchronized void init() {
		initialized = true;
		pollerManager.queue();
	}
	
	public Promise<Result> cambiarModo(String modo) throws APIException {
		MODO_SISTEMA modoEnum = MODO_SISTEMA.valueOf(modo);
		if (modoEnum == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("modo");

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		factory.setModo(modoEnum);

		return json(OK);
	}

	@ApiOperation(value = "Crear CFE",
		    response = CFE.class
		    )
	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> aceptarDocumento(String rut) throws APIException {
		// TODO meter mutex
		Empresa empresa = Empresa.findByRUT(rut, true);
		
		
		if (empresa.getFirmaDigital().getValidaHasta().before(new Date()))
			throw APIException.raise(APIErrors.FIRMA_DIGITAL_VENCIDA);
		

		JsonNode jsonNode = request().body().asJson();
		JSONObject document = new JSONObject(jsonNode.toString());

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		/*
		 * Controles de existencia de campos y carga de variables
		 */
		if (!document.has("Encabezado"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Encabezado");

		if (!document.getJSONObject("Encabezado").has("IdDoc"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("IdDoc");

		if (!document.getJSONObject("Encabezado").getJSONObject("IdDoc").has("TipoCFE"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("TipoCFE");

		if (!document.getJSONObject("Encabezado").getJSONObject("IdDoc").has("id"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("id");
		
		if (!document.getJSONObject("Encabezado").has("Emisor"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Emisor");
		
//		if (!document.getJSONObject("Encabezado").getJSONObject("Emisor").has("RUCEmisor"))
//			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("RUCEmisor");
		
		if (!document.getJSONObject("Encabezado").getJSONObject("Emisor").has("CdgDGISucur"))
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("CdgDGISucur");
		
//		String rucEmisor = document.getJSONObject("Encabezado").getJSONObject("Emisor").getString("RUCEmisor");
		
		int cdgDGISucur = document.getJSONObject("Encabezado").getJSONObject("Emisor").getInt("CdgDGISucur");
		
		Sucursal sucursal = Sucursal.findByCodigoSucursal(empresa.getSucursales(), cdgDGISucur, true);
		
//		if (rucEmisor.compareTo(rut)!=0)
//			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("rucEmisor distinto del rut de la URL");

		TipoDoc tipo = TipoDoc
				.fromInt(document.getJSONObject("Encabezado").getJSONObject("IdDoc").getInt("TipoCFE"));

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoCFE");

		String id = document.getJSONObject("Encabezado").getJSONObject("IdDoc").getString("id");

		CFE cfe = CFE.findByGeneradorId(empresa, id, DireccionDocumento.EMITIDO);

		if (cfe != null)
			throw APIException.raise(APIErrors.EXISTE_CFE).withParams("generadorId", id);
		/*
		 * Fin controles de existencia de campos y carga de variables
		 */
		
		
		switch (tipo) {
		case eFactura:
		case eTicket:
			cfe = factory.getCFEMicroController(empresa).create(tipo, document, true);
			break;
		case eResguardo:
		case Nota_de_Credito_de_eFactura:
		case Nota_de_Credito_de_eTicket:
		case Nota_de_Debito_de_eFactura:
		case Nota_de_Debito_de_eTicket:
			if (!document.has("Referencia"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("Referencia");
			cfe = factory.getCFEMicroController(empresa).create(tipo, document, true);
			break;
		case eFactura_Venta_por_Cuenta_Ajena:
		case eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena:	
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena:
			if (!document.has("CompFiscal"))
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("CompFiscal");
			cfe = factory.getCFEMicroController(empresa).create(tipo, document, true);
			break;
		case eFactura_Contingencia:
		case eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case eFactura_Exportacion:
		case eFactura_Exportacion_Contingencia:
		case eRemito:
		case eRemito_Contingencia:
		case eRemito_de_Exportacion:
		case eRemito_de_Exportacion_Contingencia:
		case eResguardo_Contingencia:
		case Nota_de_Credito_de_eFactura_Contingencia:
		case Nota_de_Credito_de_eFactura_Exportacion:
		case Nota_de_Credito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Credito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Credito_de_eTicket_Contingencia:
		case Nota_de_Credito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eFactura_Contingencia:
		case Nota_de_Debito_de_eFactura_Exportacion:
		case Nota_de_Debito_de_eFactura_Exportacion_Contingencia:
		case Nota_de_Debito_de_eFactura_Venta_por_Cuenta_Ajena_Contingencia:
		case Nota_de_Debito_de_eTicket_Contingencia:
		case Nota_de_Debito_de_eTicket_Venta_por_Cuenta_Ajena_Contingencia:
		case eTicket_Contingencia:
		case eTicket_Venta_por_Cuenta_Ajena_Contingencia:
			throw APIException.raise(APIErrors.NOT_SUPPORTED).setDetailMessage(tipo.toString());
		}

		if (cfe != null) {

			if (document.has("Adenda"))
				cfe.setAdenda(document.getJSONArray("Adenda").toString());

			cfe.setSucursal(sucursal);
			
			JSONObject error = null;

			if (cfe.getFechaEmision()==null)
				cfe.setFechaEmision(new Date());
			
			try {
				factory.getServiceMicroController(empresa).enviar(cfe);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}

			JSONObject result = procesar_resultado(empresa, cfe, error);

			return json(result.toString());
		} else
			throw APIException.raise(APIErrors.NO_CFE_CREATED);
	}

	/**
	 * @param empresa
	 * @param cfe
	 * @param error
	 * @return
	 * @throws APIException
	 */
	private JSONObject procesar_resultado(Empresa empresa, CFE cfe, JSONObject error) throws APIException {
		JSONObject result;

		if (error != null) {
			JSONObject cfeJson = new JSONObject();
			cfeJson.put("nro", cfe.getNro());
			cfeJson.put("serie", cfe.getSerie());
			result = JSONUtils.merge(cfeJson, error);
		}
		else {
			JSONObject cfeJson;
			try {
				cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);
			} catch (JSONException e) {
				throw APIException.raise(APIErrors.BAD_JSON);
			}
			result = JSONUtils.merge(cfeJson, new JSONObject(OK));
		}
		
		try {
			generarPDF(empresa, cfe);
		} catch (Throwable e) {
		}
		return result;
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Promise<Result> reenviarDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {
		// TODO meter mutex
		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);

		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
		
		CFE cfe = cfes.get(0);
		
		JSONObject error = null;

		try {
			if (cfe.getEstado() == null)
				factory.getServiceMicroController(empresa).reenviar(cfe.getSobreEmitido());
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		JSONObject result = procesar_resultado(empresa, cfe, error);
		
		return json(result.toString());
	}

	public Promise<Result> anularDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);

		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
		
		CFE cfe = cfes.get(0);
		
		factory.getServiceMicroController(empresa).anularDocumento(cfe);

		return json(OK);
	}

	public Promise<Result> resultadoDocumentosFecha(String rut, String fecha) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		Date date = DateHandler.fromStringToDate(fecha, new SimpleDateFormat("yyyyMMdd"));

		factory.getServiceMicroController(empresa).consultarResultados(date);

		return json(OK);

	}

	public Promise<Result> resultadoDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);

		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
		
		CFE cfe = cfes.get(0);
		
		JSONObject error = null;

		try {
			if (cfe.getEstado() == null)
				factory.getServiceMicroController(empresa).consultaResultado(cfe.getSobreEmitido());
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
	
	

	
	public Promise<Result> enviarCfeEmpresa(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);

		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
		
		CFE cfe = cfes.get(0); 
			
		JSONObject error = null;

		try {
			factory.getServiceMicroController(empresa).enviarCfeEmpresa(cfe);
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}

	public Promise<Result> procesarEmailEntrantes(String rut) throws APIException {
		Empresa empresaReceptora = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		factory.getServiceMicroController(empresaReceptora).obtenerYProcesarEmailsEntrantesDesdeServerCorreo();
		
		return json(OK);

	}
	
	public Promise<Result> getDocumentos(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Date fromDate = request().getQueryString("fromDate") != null ? (new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000)) : null;
		Date toDate = request().getQueryString("toDate") != null ? (new Date(Long.parseLong(request().getQueryString("toDate")) * 1000)) : null;

		int page = request().getQueryString("page") != null ? Integer.parseInt(request().getQueryString("page")) : 1;
		int pageSize = request().getQueryString("pageSize") != null ? Math.min(Integer.parseInt(request().getQueryString("pageSize")), 50) : 50;

		Integer nro = request().getQueryString("nro") != null ? Integer.parseInt(request().getQueryString("nro")) : null;
		Integer idTipoDoc = request().getQueryString("idTipoDoc") != null ? Integer.parseInt(request().getQueryString("idTipoDoc")) : null;
		String serie = request().getQueryString("serie") != null ? request().getQueryString("serie") : null;

		String rutExterno = request().getQueryString("rutExterno") != null ? request().getQueryString("rutExterno") : null;
		//String razonReceptor = request().getQueryString("razonReceptor") != null ? request().getQueryString("razonReceptor") : null;

		DireccionDocumento direccion = request().getQueryString("direccion") != null ? DireccionDocumento.valueOf(request().getQueryString("direccion")) : DireccionDocumento.EMITIDO;
		
		if (idTipoDoc != null && nro != null && serie != null) {
			/*
			 * Solo un documento
			 */
			TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

			if (tipo == null)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);
			
			List<CFE> cfes = null;
			
			if (direccion == DireccionDocumento.EMITIDO) {
				cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);
			}else {
				if (direccion == DireccionDocumento.RECIBIDO) {
					Empresa empresaEmisora = Empresa.findByRUT(rutExterno, true);
				
					cfes = CFE.findById(empresaEmisora, tipo, serie, nro, null, empresa, true, direccion);
				} 
			}
				
			if (cfes.size()==0)
				throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No exite CFE");
			
			if (cfes.size()>1)
				throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
			
			CFE cfe = cfes.get(0);
			
			JSONObject cfeObject = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

			JSONArray cfeArray = new JSONArray();
			cfeArray.put(cfeObject);
			
			return json(JSONUtils.createObjectList(cfeArray, 1, 1, 1).toString());
			
		}else {
			/*
			 * Una lista de documentos
			 */
			if (page <=0)
				page = 1;
			if (pageSize <=0)
				pageSize = 10;
			
			Empresa empresaExterna = null;
			
			if (rutExterno != null) {
				empresaExterna = Empresa.findByRUT(rutExterno, true);
			}
			
			Tuple<List<CFE>, Long> cfes;
			
			if (direccion == DireccionDocumento.EMITIDO)
				cfes = CFE.find(empresa, empresaExterna, fromDate, toDate, page, pageSize, direccion);
			else
				cfes = CFE.find(empresaExterna, empresa, fromDate, toDate, page, pageSize, direccion);
			
			JSONArray cfeArray = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfes.item1);
			
			return json(JSONUtils.createObjectList(cfeArray, cfes.item2, page, pageSize).toString());
		}
	}
	
	
	
	
	
	
	
	
	public Promise<Result> getDocumento(String rut, int nro, String serie, int idTipoDoc) throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);

		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);

		if (cfes.size()>1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
		
		CFE cfe = cfes.get(0);
		
		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);

		cfeJson = JSONUtils.merge(cfeJson, new JSONObject(OK));

		return json(cfeJson.toString());
	}
	
	
	

	public Promise<Result> pdfDocumento(String rut, int nro, String serie, int idTipoDoc, boolean print, String rutEmisor)
			throws APIException {

		Empresa empresa = Empresa.findByRUT(rut, true);
		
		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
		
		if (rutEmisor.equals(rut) || rutEmisor.equals("")){
			// Es un CFE emitido
			List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);
	
			if (cfes.size()>1)
				throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
			
			CFE cfe = cfes.get(0);
			
			try {
				File pdf = generarPDF(empresa, cfe);
	
				if (print)
					Print.print(null, pdf);
	
				return Promise.<Result>pure(ok(pdf));
	
			} catch (IOException | PrinterException e) {
				throw APIException.raise(e);
			}
		}else {
			// es un CFE recibido
			
			Empresa empresaEmisora = Empresa.findByRUT(rutEmisor, true);
			
			List<CFE> cfes = CFE.findByIdRecibido(empresa, tipo, serie, nro, null, empresaEmisora, true);
			
			if (cfes.size()>1)
				throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC",rut+"-"+nro+"-"+serie+"-"+idTipoDoc).setDetailMessage("No identifica a un unico cfe");
			
			CFE cfe = cfes.get(0);
			
			try {
				File pdf = generarPDF(empresaEmisora, cfe);
	
				if (print)
					Print.print(null, pdf);
	
				return Promise.<Result>pure(ok(pdf));
	
			} catch (IOException | PrinterException e) {
				throw APIException.raise(e);
			}
			
			
			
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

		logger.info("Generando PDF empresaId:{} - tipoDoc:{} - serie:{} - nro:{}",cfe.getEmpresaEmisora().getId(), cfe.getTipo().value, cfe.getSerie(), cfe.getNro());

		GenerateInvoice generateInvoice = new GenerateInvoice(cfe);

		String filename = Commons.getPDFfilename(cfe);

		String path = Commons.getPDFpath(empresa, cfe);
		
		// Check Directory
		File directory = new File(path);
	    if (! directory.exists()){
	        directory.mkdirs();
	        // If you require it to make the entire directory path including parents,
	        // use directory.mkdirs(); here instead.
	    }
		
	    // Write file 
		File pdf = new File(path + File.separator + filename);
		ByteArrayOutputStream byteArrayOutputStream = generateInvoice.createPDF();
		try (OutputStream outputStream = new FileOutputStream(pdf)) {
			byteArrayOutputStream.writeTo(outputStream);
		}
		return pdf;
	}

	
}
