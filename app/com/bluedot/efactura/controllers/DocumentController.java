package com.bluedot.efactura.controllers;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import com.bluedot.efactura.microControllers.implementation.ServiceMicroControllerDefault;
import com.bluedot.efactura.model.EnvioMail;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.User;
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
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de Documentos") 
public class DocumentController extends AbstractController {

	private static boolean initialized = false;
	
	final static Logger logger = LoggerFactory.getLogger(DocumentController.class);

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

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
		SobreEmitido sobre = cfe.getSobreEmitido();
		
		JSONObject error = null;

		/*
		 * consultaDgi: que paso con la consulta (YA_TENIA_RESPUESTA, SIN_SOBRE, ANTES_DE_TIEMPO, RESPONDIO,
		 * SIN_RESPUESTA)
		 */
		String consultaDgi;
		if (cfe.getEstado() != null)
			consultaDgi = "YA_TENIA_RESPUESTA";
		else if (sobre == null)
			consultaDgi = "SIN_SOBRE";
		else if (ServiceMicroControllerDefault.antesDeFechaConsulta(sobre))
			consultaDgi = "ANTES_DE_TIEMPO";
		else {
			try {
				factory.getServiceMicroController(empresa).consultaResultado(sobre);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}
			// Si al sobre le faltaban token o idReceptor se recuperan con la consulta y puede quedar antes de tiempo
			consultaDgi = cfe.getEstado() != null ? "RESPONDIO"
					: ServiceMicroControllerDefault.antesDeFechaConsulta(sobre) ? "ANTES_DE_TIEMPO" : "SIN_RESPUESTA";
		}

		JSONObject cfeJson = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(cfe);
		cfeJson.put("consultaDgi", consultaDgi);
		if (sobre != null && sobre.getFechaConsulta() != null)
			cfeJson.put("respuestaDisponibleDesde",
					new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(sobre.getFechaConsulta()));

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
			User sesion = PermissionValidator.getSessionUser(ctx());
			factory.getServiceMicroController(empresa).enviarCfeEmpresa(cfe, sesion != null ? sesion.getEmailAddress() : "API");
		} catch (APIException e) {
			logger.error("APIException:", e);
			error = e.getJSONObject();
		}

		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}

	/** El CFE emitido identificado por tipo, serie y numero (unico) */
	private static CFE cfeEmitido(Empresa empresa, int nro, String serie, int idTipoDoc) throws APIException {
		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);
		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);
		if (cfes.size() > 1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC", empresa.getRut() + "-" + nro + "-" + serie + "-" + idTipoDoc)
					.setDetailMessage("No identifica a un unico cfe");
		return cfes.get(0);
	}

	/** A donde se enviaria por mail un comprobante emitido (no envia nada) */
	public Promise<Result> destinosMail(String rut, int nro, String serie, int idTipoDoc) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);
		CFE cfe = cfeEmitido(empresa, nro, serie, idTipoDoc);
		JSONObject destinos = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory()
				.getServiceMicroController(empresa).destinosCorreo(cfe);
		return json(JSONUtils.merge(destinos, new JSONObject(OK)).toString());
	}

	/**
	 * Reenvia por mail un comprobante emitido: el XML al receptor electronico y el PDF a la direccion del comprobante.
	 * Cualquier usuario con acceso a la empresa, con auditoria. Si no hay a quien enviarlo da error.
	 */
	public Promise<Result> reenviarMail(String rut, int nro, String serie, int idTipoDoc) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());

		Empresa empresa = Empresa.findByRUT(rut, true);
		CFE cfe = cfeEmitido(empresa, nro, serie, idTipoDoc);

		List<String> enviados = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory()
				.getServiceMicroController(empresa).enviarCfeEmpresa(cfe, usuario.getEmailAddress());
		if (enviados.isEmpty())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("destino", "mail")
					.setDetailMessage("No hay a quién enviarlo: el receptor no es emisor electrónico (o no tiene mail) y el comprobante no tiene mail para el PDF");

		auditoria.info(usuario.getEmailAddress() + " reenvió por mail el comprobante " + cfe.getTipo().value + " " + cfe.getSerie() + "-"
				+ cfe.getNro() + " de la empresa " + rut + ": " + String.join(", ", enviados));

		JSONObject resultado = new JSONObject();
		resultado.put("enviados", new JSONArray(enviados));
		return json(JSONUtils.merge(resultado, new JSONObject(OK)).toString());
	}

	/**
	 * Envios por mail registrados (EnvioMail): de un comprobante emitido (idTipoDoc, serie, nro) o de un sobre (sobre),
	 * del mas nuevo al mas viejo
	 */
	public Promise<Result> enviosMail(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);
		List<EnvioMail> envios;
		try {
			if (request().getQueryString("sobre") != null)
				envios = EnvioMail.findBySobre(empresa, Long.parseLong(request().getQueryString("sobre")));
			else
				envios = EnvioMail.findByComprobante(empresa, Integer.parseInt(request().getQueryString("idTipoDoc")),
						request().getQueryString("serie"), Long.parseLong(request().getQueryString("nro")));
		} catch (NumberFormatException | NullPointerException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("Indicar sobre, o idTipoDoc, serie y nro");
		}

		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
		JSONArray lista = new JSONArray();
		for (EnvioMail e : envios) {
			JSONObject json = new JSONObject();
			json.put("id", e.getId());
			json.put("fecha", sdf.format(e.getFecha()));
			json.put("tipo", e.getTipo() != null ? e.getTipo().name() : JSONObject.NULL);
			json.put("destinatarios", e.getDestinatarios() != null ? e.getDestinatarios() : JSONObject.NULL);
			json.put("archivo", e.getArchivo() != null ? e.getArchivo() : JSONObject.NULL);
			json.put("ok", e.isOk());
			json.put("error", e.getError() != null ? e.getError() : JSONObject.NULL);
			json.put("origen", e.getOrigen() != null ? e.getOrigen() : JSONObject.NULL);
			lista.put(json);
		}
		return json(lista.toString());
	}

	public Promise<Result> procesarEmailEntrantes(String rut) throws APIException {
		Empresa empresaReceptora = Empresa.findByRUT(rut, true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();

		factory.getServiceMicroController(empresaReceptora).obtenerYProcesarEmailsEntrantesDesdeServerCorreo();
		
		return json(OK);

	}
	
	/**
	 * Sobre (emitido o recibido segun direccion) en el que viajo un CFE, con su estado y las respuestas de DGI y de la
	 * empresa receptora. Solo lectura.
	 * 
	 * Parametros: idTipoDoc, serie, nro, direccion (EMITIDO por defecto) y rutExterno (RUT del emisor, para RECIBIDO).
	 * Se busca a partir del CFE porque Sobre.findById busca en las tablas de emitidos y recibidos a la vez y un mismo
	 * id puede existir en ambas.
	 */
	//TODO agregar validacion de que el usuario tiene permisos sobre esta empresa (cubierto por @EmpresaAccess)
	/** Nombres de los parametros que identifican un comprobante y faltan, para el mensaje de MISSING_PARAMETER */
	private static String faltantes(Integer idTipoDoc, String serie, Integer nro) {
		List<String> faltan = new LinkedList<String>();
		if (idTipoDoc == null)
			faltan.add("idTipoDoc");
		if (serie == null)
			faltan.add("serie");
		if (nro == null)
			faltan.add("nro");
		return String.join(", ", faltan);
	}

	/**
	 * Comprobantes emitidos que referencian al comprobante emitido idTipoDoc/serie/nro (notas de credito o debito, por
	 * ejemplo). Solo lectura. Solo emitidos: en los recibidos la referencia no se vincula al recibir.
	 */
	public Promise<Result> getReferenciadoPor(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Integer idTipoDoc;
		Integer nro;
		try {
			idTipoDoc = request().getQueryString("idTipoDoc") != null ? Integer.parseInt(request().getQueryString("idTipoDoc")) : null;
			nro = request().getQueryString("nro") != null ? Integer.parseInt(request().getQueryString("nro")) : null;
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("idTipoDoc o nro invalidos");
		}
		String serie = request().getQueryString("serie");

		if (idTipoDoc == null || nro == null || serie == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams(faltantes(idTipoDoc, serie, nro)).setDetailMessage("idTipoDoc, serie y nro son obligatorios");

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);

		List<CFE> cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);
		if (cfes.size() != 1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC", rut + "-" + nro + "-" + serie + "-" + idTipoDoc)
					.setDetailMessage(cfes.isEmpty() ? "No exite CFE" : "No identifica a un unico cfe");

		JSONArray referencias = EfacturaJSONSerializerProvider.getCFESerializer().objectToJson(CFE.findReferenciadoPor(cfes.get(0)));

		return json(referencias.toString());
	}

	public Promise<Result> getSobreDocumento(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Integer idTipoDoc;
		Integer nro;
		DireccionDocumento direccion;
		try {
			idTipoDoc = request().getQueryString("idTipoDoc") != null ? Integer.parseInt(request().getQueryString("idTipoDoc")) : null;
			nro = request().getQueryString("nro") != null ? Integer.parseInt(request().getQueryString("nro")) : null;
			direccion = request().getQueryString("direccion") != null ? DireccionDocumento.valueOf(request().getQueryString("direccion")) : DireccionDocumento.EMITIDO;
		} catch (IllegalArgumentException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("idTipoDoc, nro o direccion invalidos");
		}
		String serie = request().getQueryString("serie");
		String rutExterno = request().getQueryString("rutExterno");

		if (idTipoDoc == null || nro == null || serie == null)
			throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams(faltantes(idTipoDoc, serie, nro)).setDetailMessage("idTipoDoc, serie y nro son obligatorios");

		TipoDoc tipo = TipoDoc.fromInt(idTipoDoc);
		if (tipo == null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);

		List<CFE> cfes;
		if (direccion == DireccionDocumento.EMITIDO)
			cfes = CFE.findByIdEmitido(empresa, tipo, serie, nro, null, true);
		else {
			if (rutExterno == null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("rutExterno").setDetailMessage("rutExterno es obligatorio para documentos recibidos");
			cfes = CFE.findById(Empresa.findByRUT(rutExterno, true), tipo, serie, nro, null, empresa, true, direccion);
		}

		if (cfes.size() != 1)
			throw APIException.raise(APIErrors.CFE_NO_ENCONTRADO).withParams("RUT+NRO+SERIE+TIPODOC", rut + "-" + nro + "-" + serie + "-" + idTipoDoc)
					.setDetailMessage(cfes.isEmpty() ? "No exite CFE" : "No identifica a un unico cfe");

		CFE cfe = cfes.get(0);
		Sobre sobre = direccion == DireccionDocumento.EMITIDO ? cfe.getSobreEmitido() : cfe.getSobreRecibido();

		JSONObject result = new JSONObject();
		result.put("direccion", direccion.name());
		result.put("sobre", sobre != null ? EfacturaJSONSerializerProvider.getSobreSerializer().objectToJson(sobre, false) : JSONObject.NULL);

		return json(result.toString());
	}

	/**
	 * Resumen de comprobantes de la empresa en un periodo, para el tablero del panel (solo lectura). Agregado en la
	 * base: no trae los comprobantes.
	 * 
	 * Parametros: fromDate / toDate (epoch en segundos, fecha de emision; por defecto los ultimos 30 dias).
	 * Mismos criterios que el listado: emitidos con sobre emitido, recibidos con sobre recibido.
	 */
	//TODO agregar validacion de que el usuario tiene permisos sobre esta empresa (cubierto por @EmpresaAccess)
	public Promise<Result> getResumenDocumentos(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		Date hasta;
		Date desde;
		try {
			hasta = request().getQueryString("toDate") != null ? new Date(Long.parseLong(request().getQueryString("toDate")) * 1000) : new Date();
			desde = request().getQueryString("fromDate") != null ? new Date(Long.parseLong(request().getQueryString("fromDate")) * 1000)
					: DateHandler.add(hasta, -29, Calendar.DAY_OF_MONTH);
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("fromDate y toDate deben ser numericos (epoch en segundos)");
		}

		javax.persistence.EntityManager em = play.db.jpa.JPA.em();
		SimpleDateFormat dia = new SimpleDateFormat("yyyy-MM-dd");

		List<Object[]> emitidos = em.createQuery("select c.tipo, c.estado, c.moneda, count(c), sum(c.totMntTotal) from CFE c"
				+ " where c.empresaEmisora = :empresa and c.sobreEmitido is not null and c.fechaEmision between :desde and :hasta"
				+ " group by c.tipo, c.estado, c.moneda", Object[].class)
				.setParameter("empresa", empresa).setParameter("desde", desde).setParameter("hasta", hasta).getResultList();

		// Recibidos sin los que la empresa rechazo (BE, ej: duplicados E02): no son compras validas
		List<Object[]> recibidos = em.createQuery("select c.tipo, c.moneda, count(c), sum(c.totMntTotal) from CFE c"
				+ " where c.empresaReceptora = :empresa and c.sobreRecibido is not null and c.fechaEmision between :desde and :hasta"
				+ " and (c.estado is null or c.estado <> :rechazado)"
				+ " group by c.tipo, c.moneda", Object[].class)
				.setParameter("empresa", empresa).setParameter("desde", desde).setParameter("hasta", hasta)
				.setParameter("rechazado", EstadoACKCFEType.BE).getResultList();

		List<Object[]> emitidosPorDia = em.createQuery("select c.fechaEmision, count(c) from CFE c"
				+ " where c.empresaEmisora = :empresa and c.sobreEmitido is not null and c.fechaEmision between :desde and :hasta"
				+ " group by c.fechaEmision", Object[].class)
				.setParameter("empresa", empresa).setParameter("desde", desde).setParameter("hasta", hasta).getResultList();

		List<Object[]> recibidosPorDia = em.createQuery("select c.fechaEmision, count(c) from CFE c"
				+ " where c.empresaReceptora = :empresa and c.sobreRecibido is not null and c.fechaEmision between :desde and :hasta"
				+ " and (c.estado is null or c.estado <> :rechazado)"
				+ " group by c.fechaEmision", Object[].class)
				.setParameter("empresa", empresa).setParameter("desde", desde).setParameter("hasta", hasta)
				.setParameter("rechazado", EstadoACKCFEType.BE).getResultList();

		JSONArray emitidosJson = new JSONArray();
		for (Object[] fila : emitidos) {
			JSONObject json = new JSONObject();
			json.put("tipo", fila[0] != null ? ((TipoDoc) fila[0]).name() : JSONObject.NULL);
			json.put("idTipoDoc", fila[0] != null ? ((TipoDoc) fila[0]).value : JSONObject.NULL);
			json.put("estado", fila[1] != null ? fila[1].toString() : JSONObject.NULL);
			json.put("moneda", fila[2] != null ? fila[2].toString() : JSONObject.NULL);
			json.put("cantidad", fila[3]);
			json.put("total", fila[4] != null ? fila[4] : 0);
			emitidosJson.put(json);
		}

		JSONArray recibidosJson = new JSONArray();
		for (Object[] fila : recibidos) {
			JSONObject json = new JSONObject();
			json.put("tipo", fila[0] != null ? ((TipoDoc) fila[0]).name() : JSONObject.NULL);
			json.put("idTipoDoc", fila[0] != null ? ((TipoDoc) fila[0]).value : JSONObject.NULL);
			json.put("moneda", fila[1] != null ? fila[1].toString() : JSONObject.NULL);
			json.put("cantidad", fila[2]);
			json.put("total", fila[3] != null ? fila[3] : 0);
			recibidosJson.put(json);
		}

		// Un elemento por dia con comprobantes: {fecha, emitidos, recibidos}
		java.util.TreeMap<String, long[]> porDia = new java.util.TreeMap<String, long[]>();
		for (Object[] fila : emitidosPorDia)
			if (fila[0] != null) {
				long[] valores = porDia.containsKey(dia.format((Date) fila[0])) ? porDia.get(dia.format((Date) fila[0])) : new long[2];
				valores[0] += (Long) fila[1];
				porDia.put(dia.format((Date) fila[0]), valores);
			}
		for (Object[] fila : recibidosPorDia)
			if (fila[0] != null) {
				long[] valores = porDia.containsKey(dia.format((Date) fila[0])) ? porDia.get(dia.format((Date) fila[0])) : new long[2];
				valores[1] += (Long) fila[1];
				porDia.put(dia.format((Date) fila[0]), valores);
			}
		JSONArray porDiaJson = new JSONArray();
		for (java.util.Map.Entry<String, long[]> entrada : porDia.entrySet()) {
			JSONObject json = new JSONObject();
			json.put("fecha", entrada.getKey());
			json.put("emitidos", entrada.getValue()[0]);
			json.put("recibidos", entrada.getValue()[1]);
			porDiaJson.put(json);
		}

		JSONObject result = new JSONObject();
		result.put("desde", dia.format(desde));
		result.put("hasta", dia.format(hasta));
		result.put("emitidos", emitidosJson);
		result.put("recibidos", recibidosJson);
		result.put("porDia", porDiaJson);

		return json(result.toString());
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
		String rutReceptor = request().getQueryString("rutReceptor") != null ? request().getQueryString("rutReceptor") : null;
		String razonReceptor = request().getQueryString("razonReceptor") != null ? request().getQueryString("razonReceptor") : null;

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
			
			/*
			 * Sin tipo, serie y numero juntos se lista: idTipoDoc, serie y nro, si vienen, filtran el listado (ej. buscar un
			 * numero en todos los tipos y series)
			 */
			TipoDoc tipoFiltro = null;
			if (idTipoDoc != null) {
				tipoFiltro = TipoDoc.fromInt(idTipoDoc);
				if (tipoFiltro == null)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("TipoDoc", idTipoDoc);
			}
			
			/*
			 * Estado de la respuesta de DGI: AE (aceptado), BE (rechazado), CE (observado) o SIN (sin respuesta)
			 */
			String estadoFiltro = request().getQueryString("estado");
			if (estadoFiltro != null && !estadoFiltro.trim().isEmpty() && !estadoFiltro.trim().matches("(?i)AE|BE|CE|SIN"))
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("estado", estadoFiltro);
			
			Tuple<List<CFE>, Long> cfes;
			
			if (direccion == DireccionDocumento.EMITIDO)
				cfes = CFE.find(empresa, empresaExterna, rutReceptor, razonReceptor, tipoFiltro, serie, nro, estadoFiltro, fromDate, toDate, page, pageSize, direccion);
			else
				cfes = CFE.find(empresaExterna, empresa, rutReceptor, razonReceptor, tipoFiltro, serie, nro, estadoFiltro, fromDate, toDate, page, pageSize, direccion);
			
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
