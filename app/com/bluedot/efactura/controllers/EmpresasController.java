package com.bluedot.efactura.controllers;

import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.ThreadMan;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.empresas.CargaFirmaDigital;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
import com.bluedot.efactura.model.Sucursal;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.bluedot.efactura.services.ConsultaRutService;
import com.bluedot.efactura.services.impl.ConsultaRutServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de Empresa") 
public class EmpresasController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(EmpresasController.class);

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

//	private Mutex mutex;
//	
//	@Inject
//	public EmpresasController(Mutex mutex){
//		this.mutex = mutex;
//	}
	
	public Promise<Result> darInformacionRut(String idrut) throws APIException {
		//TODO tomar este consulta rut de un factory
		ConsultaRutService service = new ConsultaRutServiceImpl();

		// Call the service
		String response = service.getRutData(idrut);

		return json(response);
	}

	public Promise<Result> cargarEmpresas(String path) throws APIException {
		try {

			File fXmlFile = IO.resolve(path);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc;
			// El archivo de emisores es muy grande, se versiona comprimido (.gz)
			if (path.toLowerCase().endsWith(".gz")) {
				try (InputStream in = new GZIPInputStream(new FileInputStream(fXmlFile))) {
					doc = dBuilder.parse(in);
				}
			} else
				doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("RucEmisoresTransicionMailProv.RucEmisoresTransicionMailProvItem");

			List<Empresa> empresas = Empresa.findAll();
			
			HashMap<String, Empresa> empresasMap = new HashMap<String, Empresa>();
			
			for (Empresa empresa : empresas) {
				empresasMap.put(empresa.getRut(), empresa);
			}
			
			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String rut = eElement.getElementsByTagName("RUC").item(0).getTextContent();

					logger.info("Procesando RUT: " + rut + " " + (temp + 1) + "/" + nList.getLength());

					String denominacion = eElement.getElementsByTagName("DENOMINACION").item(0).getTextContent();
					//String fechaInicio = eElement.getElementsByTagName("FECHA_INICIO").item(0).getTextContent();
					String mail = eElement.getElementsByTagName("MAIL").item(0).getTextContent();

					Empresa empresa = empresasMap.get(rut);
					
					if ( empresa == null) {
						empresa = new Empresa(rut, null, null);
						empresa.setEmisorElectronico(true);
						empresa.setMailRecepcion(mail);
						empresa.setRazon(denominacion);
						empresasMap.put(rut, empresa);
						empresa.save();
					}else{
						if (!empresa.isEmisorElectronico() || !Objects.equals(empresa.getMailRecepcion(), mail) || !Objects.equals(empresa.getRazon(), denominacion)) {
							empresa.setEmisorElectronico(true);
							empresa.setMailRecepcion(mail);
							empresa.setRazon(denominacion);
							empresa.update();
						}
					}
					
					if (temp % 1000 == 0) 
						ThreadMan.forceTransactionFlush();

				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return json(OK);
	}
	
	public Promise<Result> getEmpresaById(int id) throws APIException {
		
		Empresa empresa = Empresa.findById(id, true);
		
		JSONObject json = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresa);
		
		return json(json.toString());
	}
	
	@ApiOperation(value = "Buscar Empresa por Rut",
		    response = Empresa.class
		    )
	public Promise<Result> getEmpresaByRut(String rut) throws APIException {
		
		Empresa empresa = Empresa.findByRUT(rut, true);
		
		JSONObject json = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresa);
		
		return json(json.toString());
	}
	
	/**
	 * Lista de empresas.
	 *
	 * Con page, pageSize o algun filtro (q, emisorElectronico, conFirma) devuelve una pagina
	 * {results, count, page, pageSize} ordenada por rut (pageSize maximo 100). 
	 */
	public Promise<Result> getEmpresas() throws APIException {

		String pageParam = request().getQueryString("page");
		String pageSizeParam = request().getQueryString("pageSize");
		String texto = request().getQueryString("q");
		String emisorElectronicoParam = request().getQueryString("emisorElectronico");
		boolean conFirma = "true".equalsIgnoreCase(request().getQueryString("conFirma"));

		int page;
		int pageSize;
		try {
			page = pageParam != null ? Integer.parseInt(pageParam) : 1;
			pageSize = pageSizeParam != null ? Integer.parseInt(pageSizeParam) : 20;
		} catch (NumberFormatException e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("page/pageSize", pageParam + "/" + pageSizeParam);
		}
		if (page <= 0)
			page = 1;
		if (pageSize <= 0)
			pageSize = 20;
		pageSize = Math.min(pageSize, 100);

		Boolean emisorElectronico = emisorElectronicoParam != null ? Boolean.valueOf(emisorElectronicoParam) : null;

		// Control de acceso: un usuario que no es ADMIN solo ve sus empresas (en modo log solo se registra)
		List<String> ruts = null;
		AccesoEmpresa.Modo modo = AccesoEmpresa.modo();
		if (modo != AccesoEmpresa.Modo.OFF) {
			User user = PermissionValidator.getSessionUser(ctx());
			if (!AccesoEmpresa.esAdmin(user)) {
				List<String> permitidas = AccesoEmpresa.rutsPermitidos(user);
				if (modo == AccesoEmpresa.Modo.ENFORCE)
					ruts = permitidas;
				else
					logger.warn("SERIA FILTRADO: usuario " + (user != null ? user.getEmailAddress() : "desconocido")
							+ " lista empresas con acceso a " + permitidas.size() + " empresas (GET /api/v1/empresa)");
			}
		}

		Tuple<List<Empresa>, Long> empresas = Empresa.find(texto, emisorElectronico, conFirma, ruts, page, pageSize);

		JSONArray json = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresas.item1);

		return json(JSONUtils.createObjectList(json, empresas.item2, page, pageSize).toString());
	}


	public Promise<Result> crearEmpresa() throws APIException
	{
		JsonNode empresaJson = request().body().asJson();
		Empresa empresa = new Empresa();
		String rut = empresaJson.has("rut") ? empresaJson.findPath("rut").asText() : null;
		String nombreComercial = empresaJson.has("nombreComercial") ? empresaJson.findPath("nombreComercial").asText() : null;
		String logoPath = empresaJson.has("logoPath") ? empresaJson.findPath("logoPath").asText() : null;
		String paginaWeb = empresaJson.has("paginaWeb") ? empresaJson.findPath("paginaWeb").asText() : null;
		String resolucion = empresaJson.has("resolucion") ? empresaJson.findPath("resolucion").asText() : null;
		String razon = empresaJson.has("razon") ? empresaJson.findPath("razon").asText() : null;

		String hostRecepcion = empresaJson.has("hostRecepcion") ? empresaJson.findPath("hostRecepcion").asText() : null;
		String passRecepcion = empresaJson.has("passRecepcion") ? empresaJson.findPath("passRecepcion").asText() : null;
		String puertoRecepcion = empresaJson.has("puertoRecepcion") ? empresaJson.findPath("puertoRecepcion").asText() : null;
		String userRecepcion = empresaJson.has("userRecepcion") ? empresaJson.findPath("userRecepcion").asText() : null;
		String mailNotificaciones = empresaJson.has("mailNotificaciones") ? empresaJson.findPath("mailNotificaciones").asText() : null;
		String mailRecepcion = empresaJson.has("mailRecepcion") ? empresaJson.findPath("mailRecepcion").asText() : null;
		String fromEnvio = empresaJson.has("fromEnvio") ? empresaJson.findPath("fromEnvio").asText() : null;

		String certificado = empresaJson.has("certificado") ? empresaJson.findPath("certificado").asText() : null;
		String privateKey = empresaJson.has("privateKey") ? empresaJson.findPath("privateKey").asText() : null;

		if (hostRecepcion != null)
			empresa.setHostRecepcion(hostRecepcion);

		if (rut != null)
			empresa.setRut(rut);

		if (passRecepcion != null)
			empresa.setPassRecepcion(passRecepcion);

		if (puertoRecepcion != null)
			empresa.setPuertoRecepcion(puertoRecepcion);

		if (userRecepcion != null)
			empresa.setUserRecepcion(userRecepcion);

		if (mailNotificaciones != null)
			empresa.setMailNotificaciones(mailNotificaciones);

		if (mailRecepcion != null)
			empresa.setMailRecepcion(mailRecepcion);

		if (fromEnvio != null)
			empresa.setFromEnvio(fromEnvio);

		if (paginaWeb != null)
			empresa.setPaginaWeb(paginaWeb);

		if (nombreComercial != null)
			empresa.setNombreComercial(nombreComercial);

		if (resolucion != null)
			empresa.setResolucion(resolucion);

		if (razon != null)
			empresa.setRazon(razon);


		if (logoPath!=null){
			Path path = IO.resolve(logoPath).toPath();
			try {
				byte[] logo = Files.readAllBytes(path);
				empresa.setLogo(logo);
			} catch (IOException e) {
			}

		}

		if (certificado != null){
			if (privateKey ==null)
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("privateKey").setDetailMessage("privateKey");

			FirmaDigital firmaDigital = new FirmaDigital();

			firmaDigital.setCertificate(certificado);
			firmaDigital.setPrivateKey(privateKey);
			try {

				KeyStore keystore = firmaDigital.getKeyStore();


				if(keystore.getCertificate(FirmaDigital.KEY_ALIAS).getType().equals("X.509")){
					X509Certificate certificate  = (X509Certificate) keystore.getCertificate(FirmaDigital.KEY_ALIAS);

					firmaDigital.setValidaHasta(certificate.getNotAfter());
					firmaDigital.setValidaDesde(certificate.getNotBefore());

					if (empresa.getFirmaDigital()!=null)
						empresa.getFirmaDigital().delete();

					firmaDigital.setEmpresa(empresa);
					firmaDigital.save();

				}
			} catch (IOException | GeneralSecurityException e) {
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).setDetailMessage("certificado invalido o clave privada invalida");
			}


		}

		empresa.save();

		return json(OK);
	}

	/** Tamano maximo del logo (va en el PDF de cada comprobante) */
	private static final int MAX_LOGO = 512 * 1024;

	/**
	 * Edita los datos de una empresa. Solo ADMIN. Cada campo es opcional: los que no vienen no cambian.
	 * <ul>
	 * <li>razon, nombreComercial, paginaWeb, resolucion</li>
	 * <li>mailRecepcion, hostRecepcion, puertoRecepcion, userRecepcion, passRecepcion (nunca se devuelve),
	 * mailNotificaciones, fromEnvio</li>
	 * <li>logo: imagen PNG o JPEG en base64 (hasta 512 KB)</li>
	 * <li>firma digital: certificado + privateKey (PEM), o pkcs12 (archivo .p12/.pfx en base64) + pkcs12Password; si el
	 * certificado no menciona el RUT de la empresa se rechaza salvo forzarFirma=true</li>
	 * </ul>
	 * Devuelve la empresa y, si se cargo firma, sus datos (titular, emisor, validez, rutEnCertificado).
	 */
	public Promise<Result> editarEmpresa(String rut) throws APIException
	{
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "PUT /empresa/" + rut, true);

		Empresa empresa = Empresa.findByRUT(rut,true);
		
		JsonNode empresaJson = request().body().asJson();
		if (empresaJson == null)
			throw APIException.raise(APIErrors.BAD_JSON);

		java.util.List<String> cambios = new java.util.ArrayList<String>();

		if (texto(empresaJson, "razon") != null && !Objects.equals(texto(empresaJson, "razon"), empresa.getRazon())) {
			empresa.setRazon(texto(empresaJson, "razon"));
			cambios.add("razon");
		}
		if (texto(empresaJson, "nombreComercial") != null && !Objects.equals(texto(empresaJson, "nombreComercial"), empresa.getNombreComercial())) {
			empresa.setNombreComercial(texto(empresaJson, "nombreComercial"));
			cambios.add("nombreComercial");
		}
		if (texto(empresaJson, "paginaWeb") != null && !Objects.equals(texto(empresaJson, "paginaWeb"), empresa.getPaginaWeb())) {
			empresa.setPaginaWeb(texto(empresaJson, "paginaWeb"));
			cambios.add("paginaWeb");
		}
		if (texto(empresaJson, "resolucion") != null && !Objects.equals(texto(empresaJson, "resolucion"), empresa.getResolucion())) {
			empresa.setResolucion(texto(empresaJson, "resolucion"));
			cambios.add("resolucion");
		}
		if (texto(empresaJson, "mailRecepcion") != null && !Objects.equals(texto(empresaJson, "mailRecepcion"), empresa.getMailRecepcion())) {
			empresa.setMailRecepcion(texto(empresaJson, "mailRecepcion"));
			cambios.add("mailRecepcion");
		}
		if (texto(empresaJson, "hostRecepcion") != null && !Objects.equals(texto(empresaJson, "hostRecepcion"), empresa.getHostRecepcion())) {
			empresa.setHostRecepcion(texto(empresaJson, "hostRecepcion"));
			cambios.add("hostRecepcion");
		}
		if (texto(empresaJson, "puertoRecepcion") != null && !Objects.equals(texto(empresaJson, "puertoRecepcion"), empresa.getPuertoRecepcion())) {
			empresa.setPuertoRecepcion(texto(empresaJson, "puertoRecepcion"));
			cambios.add("puertoRecepcion");
		}
		if (texto(empresaJson, "userRecepcion") != null && !Objects.equals(texto(empresaJson, "userRecepcion"), empresa.getUserRecepcion())) {
			empresa.setUserRecepcion(texto(empresaJson, "userRecepcion"));
			cambios.add("userRecepcion");
		}
		// La contrasena nunca se devuelve: vacia o ausente = no cambia
		String passRecepcion = texto(empresaJson, "passRecepcion");
		if (passRecepcion != null && !passRecepcion.isEmpty()) {
			empresa.setPassRecepcion(passRecepcion);
			cambios.add("passRecepcion");
		}
		if (texto(empresaJson, "mailNotificaciones") != null && !Objects.equals(texto(empresaJson, "mailNotificaciones"), empresa.getMailNotificaciones())) {
			empresa.setMailNotificaciones(texto(empresaJson, "mailNotificaciones"));
			cambios.add("mailNotificaciones");
		}
		if (texto(empresaJson, "fromEnvio") != null && !Objects.equals(texto(empresaJson, "fromEnvio"), empresa.getFromEnvio())) {
			empresa.setFromEnvio(texto(empresaJson, "fromEnvio"));
			cambios.add("fromEnvio");
		}

		// Logo: imagen en base64 (antes era logoPath, un archivo del servidor)
		String logo = texto(empresaJson, "logo");
		if (logo != null && !logo.isEmpty()) {
			byte[] imagen;
			try {
				imagen = java.util.Base64.getMimeDecoder().decode(logo.replaceFirst("^data:[^,]*,", ""));
			} catch (IllegalArgumentException e) {
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("logo", "base64");
			}
			if (imagen.length > MAX_LOGO)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("logo", "tamaño")
						.setDetailMessage("El logo no puede superar los 512 KB");
			if (tipoImagen(imagen) == null)
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("logo", "formato")
						.setDetailMessage("El logo tiene que ser PNG o JPEG");
			empresa.setLogo(imagen);
			cambios.add("logo");
		}

		// Firma digital: PEM (certificado + privateKey) o PKCS#12 (.p12/.pfx)
		CargaFirmaDigital.Resultado firma = null;
		String certificado = texto(empresaJson, "certificado");
		String privateKey = texto(empresaJson, "privateKey");
		String pkcs12 = texto(empresaJson, "pkcs12");
		if (pkcs12 != null && !pkcs12.isEmpty())
			firma = CargaFirmaDigital.desdePkcs12(empresa, pkcs12, texto(empresaJson, "pkcs12Password"));
		else if (certificado != null && !certificado.isEmpty()) {
			if (privateKey == null || privateKey.isEmpty())
				throw APIException.raise(APIErrors.MISSING_PARAMETER).withParams("privateKey").setDetailMessage("privateKey");
			firma = CargaFirmaDigital.desdePem(empresa, certificado, privateKey);
		}
		// Un certificado sin el RUT de la empresa puede ser de otra: se carga solo si se confirma (forzarFirma)
		if (firma != null && !firma.rutEnCertificado
				&& !(empresaJson.has("forzarFirma") && empresaJson.get("forzarFirma").asBoolean()))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("firmaDigital", "rut")
					.setDetailMessage("El certificado no menciona el RUT " + rut + " (titular: "
							+ firma.certificado.getSubjectX500Principal().getName() + "). Para cargarlo igual enviar forzarFirma");
		if (firma != null) {
			if (empresa.getFirmaDigital() != null)
				empresa.getFirmaDigital().delete();
			firma.firma.setEmpresa(empresa);
			firma.firma.save();
			empresa.setFirmaDigital(firma.firma);
			cambios.add("firmaDigital (válida hasta " + new java.text.SimpleDateFormat("yyyy-MM-dd").format(firma.firma.getValidaHasta()) + ")");
		}

		empresa.update();

		if (!cambios.isEmpty())
			auditoria.info(usuario.getEmailAddress() + " modificó la empresa " + rut + ": " + String.join(", ", cambios));

		JSONObject respuesta = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresa);
		respuesta.put("cambios", new JSONArray(cambios));
		if (firma != null) {
			JSONObject datosFirma = new JSONObject();
			datosFirma.put("titular", firma.certificado.getSubjectX500Principal().getName());
			datosFirma.put("emisor", firma.certificado.getIssuerX500Principal().getName());
			datosFirma.put("rutEnCertificado", firma.rutEnCertificado);
			respuesta.put("firmaCargada", datosFirma);
		}
		return json(JSONUtils.merge(respuesta, new JSONObject(OK)).toString());
	}

	/** Texto de un campo, sin espacios al borde; null si no viene */
	private static String texto(JsonNode json, String campo) {
		return json.has(campo) && !json.get(campo).isNull() ? json.get(campo).asText().trim() : null;
	}

	/** Tipo MIME de la imagen por su firma (magic bytes): PNG o JPEG; null si no es ninguna */
	private static String tipoImagen(byte[] imagen) {
		if (imagen.length > 8 && (imagen[0] & 0xFF) == 0x89 && imagen[1] == 'P' && imagen[2] == 'N' && imagen[3] == 'G')
			return "image/png";
		if (imagen.length > 3 && (imagen[0] & 0xFF) == 0xFF && (imagen[1] & 0xFF) == 0xD8 && (imagen[2] & 0xFF) == 0xFF)
			return "image/jpeg";
		return null;
	}

	/** Logo de la empresa (el que va en los PDF), 404 si no tiene */
	public Promise<Result> getLogo(String rut) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut, true);
		byte[] logo = empresa.getLogo();
		if (logo == null || logo.length == 0)
			return Promise.<Result>pure(notFound());
		String tipo = tipoImagen(logo);
		return Promise.<Result>pure(ok(logo).as(tipo != null ? tipo : "application/octet-stream"));
	}
	
	/*
	 * SUCURSALES
	 */
	
	public Promise<Result> getSucursales(String rut) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut,true);
		
		JSONArray json = EfacturaJSONSerializerProvider.getSucursalSerializer().objectToJson(empresa.getSucursales());
		
		return json(json.toString());
	}
	
	// TODO permisis de edicion
	public Promise<Result> editarSucursal(String rut, Integer codigoSuc) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "PUT /empresa/" + rut + "/sucursales/" + codigoSuc, true);

		Empresa empresa = Empresa.findByRUT(rut, true);
		
		Sucursal sucursal = Sucursal.findByCodigoSucursal(empresa.getSucursales(), codigoSuc, true);

		JsonNode sucursalJson = request().body().asJson();

		if (Commons.safeGetString(sucursalJson, "Telefono", false) != null)
			sucursal.setTelefono(Commons.safeGetString(sucursalJson, "Telefono", false));
		
		if (Commons.safeGetString(sucursalJson, "CodigoPostal", false) != null)
			sucursal.setCodigoPostal(Commons.safeGetString(sucursalJson, "CodigoPostal", false));
		
		if (Commons.safeGetString(sucursalJson, "CdgDGISucur", false) != null)
			sucursal.setCodigoSucursal(Commons.safeGetInteger(sucursalJson, "CdgDGISucur", false));
		
		if (Commons.safeGetString(sucursalJson, "Departamento", false) != null)
			sucursal.setDepartamento(Commons.safeGetString(sucursalJson, "Departamento", false));
		
		if (Commons.safeGetString(sucursalJson, "DomFiscal", false) != null)
			sucursal.setDomicilioFiscal(Commons.safeGetString(sucursalJson, "DomFiscal", false));
		
		if (Commons.safeGetString(sucursalJson, "Ciudad", false) != null)
			sucursal.setCiudad(Commons.safeGetString(sucursalJson, "Ciudad", false));

		sucursal.update();

		auditoria.info(usuario.getEmailAddress() + " modificó la sucursal " + codigoSuc + " de la empresa " + rut);

		return json(OK);
	}
	
	// TODO permisis de creacion
	public Promise<Result> crearSucursal(String rut) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "POST /empresa/" + rut + "/sucursales", true);

		Empresa empresa = Empresa.findByRUT(rut, true);
		
		JsonNode sucursalJson = request().body().asJson();
		
		Integer codigoSucursal = Commons.safeGetInteger(sucursalJson, "CdgDGISucur", false);
		
		Sucursal sucursal = Sucursal.findByCodigoSucursal(empresa.getSucursales(), codigoSucursal);
		
		if (sucursal!=null)
			throw APIException.raise(APIErrors.SUCURSAL_EXISTE).withParams("CdgDGISucur = " + codigoSucursal);
		else
			sucursal = new Sucursal();
		
		sucursal.setTelefono(Commons.safeGetString(sucursalJson, "Telefono", false));
		sucursal.setCodigoPostal(Commons.safeGetString(sucursalJson, "CodigoPostal", false));
		sucursal.setCodigoSucursal(codigoSucursal);
		sucursal.setDepartamento(Commons.safeGetString(sucursalJson, "Departamento", true));
		sucursal.setDomicilioFiscal(Commons.safeGetString(sucursalJson, "DomFiscal", true));
		sucursal.setCiudad(Commons.safeGetString(sucursalJson, "Ciudad", true));
		sucursal.setEmpresa(empresa);
		
		sucursal.save();
		empresa.getSucursales().add(sucursal);
		
		empresa.update();

		auditoria.info(usuario.getEmailAddress() + " creó la sucursal " + codigoSucursal + " de la empresa " + rut);

		return json(OK);
	}

}
