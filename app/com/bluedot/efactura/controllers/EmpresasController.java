package com.bluedot.efactura.controllers;

import com.bluedot.efactura.commons.Orden;
import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;


import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.commons.FormatosDgi;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.commons.security.User;
import com.bluedot.commons.utils.IO;
import com.bluedot.commons.utils.JSONUtils;
import com.bluedot.commons.utils.Tuple;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.empresas.CargaFirmaDigital;
import com.bluedot.efactura.model.CFE;
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

	
	/**
	 * Datos de un RUT en DGI, en JSON (ver DatosRutDgi) o, con formato=xml, el XML que devuelve DGI tal cual. La consulta
	 * se firma con el certificado de una empresa: la del parametro empresa (RUT, el usuario tiene que tener acceso) o, si
	 * no viene, la primera con firma digital vigente a la que el usuario tiene acceso.
	 */
	public Promise<Result> darInformacionRut(String idrut) throws APIException {
		if (idrut == null || !idrut.trim().matches("\\d{1,12}"))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("rut", idrut).setDetailMessage("El RUT tiene que ser numérico, de hasta 12 dígitos");
		idrut = idrut.trim();
		User usuario = PermissionValidator.getSessionUser(ctx());
		Empresa firmante;
		String rutFirmante = request().getQueryString("empresa");
		if (rutFirmante != null && !rutFirmante.trim().isEmpty()) {
			AccesoEmpresa.verificarEmpresa(usuario, rutFirmante.trim(), "GET /rut/" + idrut);
			firmante = Empresa.findByRUT(rutFirmante.trim(), true);
		} else
			firmante = empresaFirmante(usuario);

		String xml = new ConsultaRutServiceImpl().getRutData(idrut, firmante);
		if (xml == null || xml.trim().isEmpty())
			throw APIException.raise(APIErrors.RUT_DGI).setDetailMessage("DGI no devolvió datos para el RUT " + idrut);

		if ("xml".equalsIgnoreCase(request().getQueryString("formato")))
			return Promise.<Result> pure(ok(xml).as("application/xml; charset=utf-8"));
		try {
			return json(com.bluedot.efactura.empresas.DatosRutDgi.aJson(xml).toString());
		} catch (Exception e) {
			logger.error("No se pudo leer la respuesta de DGI para el RUT " + idrut + ": " + xml, e);
			throw APIException.raise(APIErrors.ERROR_COMUNICACION_DGI, e).setDetailMessage("No se pudo leer la respuesta de DGI");
		}
	}

	/** Primera empresa con firma digital vigente a la que el usuario tiene acceso; null si no hay */
	private static Empresa empresaFirmante(User usuario) {
		java.util.Date ahora = new java.util.Date();
		for (Empresa e : Empresa.find(null, null, true, null, 1, 1000).item1)
			if (e.getFirmaDigital() != null && e.getFirmaDigital().getValidaHasta() != null && e.getFirmaDigital().getValidaHasta().after(ahora)
					&& AccesoEmpresa.tieneAcceso(usuario, e.getRut()))
				return e;
		return null;
	}

	/**
	 * Vista previa de la sincronizacion de emisores electronicos con DGI (ver EmisoresElectronicos): lo que cambiaria
	 * desde la fecha, sin guardar nada. Query: desde (yyyy-MM-dd, obligatorio), empresa (RUT con cuya firma se consulta;
	 * si no, la primera con firma vigente). Solo ADMIN.
	 */
	public Promise<Result> cambiosEmisoresElectronicos() throws APIException {
		return emisoresElectronicos(true, "GET /emisoresElectronicos/cambios");
	}

	/** Sincroniza los emisores electronicos con DGI desde la fecha (crea y actualiza empresas). Solo ADMIN. */
	public Promise<Result> sincronizarEmisoresElectronicos() throws APIException {
		return emisoresElectronicos(false, "POST /emisoresElectronicos/sincronizar");
	}

	private Promise<Result> emisoresElectronicos(boolean simular, String operacion) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, operacion, true);
		java.util.Date desde;
		try {
			desde = new java.text.SimpleDateFormat("yyyy-MM-dd").parse(request().getQueryString("desde"));
		} catch (Exception e) {
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("desde", request().getQueryString("desde"))
					.setDetailMessage("desde tiene que ser una fecha yyyy-MM-dd");
		}
		String rutFirmante = request().getQueryString("empresa");
		Empresa firmante = rutFirmante != null && !rutFirmante.trim().isEmpty() ? Empresa.findByRUT(rutFirmante.trim(), true)
				: com.bluedot.efactura.empresas.EmisoresElectronicos.firmantePorDefecto();
		org.json.JSONObject resultado = simular ? com.bluedot.efactura.empresas.EmisoresElectronicos.sincronizar(desde, firmante, true)
				: com.bluedot.efactura.empresas.EmisoresElectronicos.ejecutar(desde, firmante, usuario.getEmailAddress());
		if (!simular)
			auditoria.info(usuario.getEmailAddress() + " sincronizo los emisores electronicos con DGI desde "
					+ request().getQueryString("desde") + ": " + resultado.getInt("nuevas") + " nuevas, " + resultado.getInt("actualizadas")
					+ " actualizadas, " + resultado.getInt("bajas") + " bajas");
		return json(resultado.toString());
	}

	/**
	 * Carga el archivo de emisores de DGI (RucEmisoresTransicionMailProv, XML o .gz) que esta en el servidor: crea y
	 * actualiza empresas con la misma logica que la sincronizacion (ver EmisoresElectronicos). Query: path, simular=true
	 * para ver lo que cambiaria sin guardar. Solo ADMIN.
	 */
	public Promise<Result> cargarEmpresas(final String path) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "POST /empresa/cargar", true);
		final File archivo = IO.resolve(path);
		if (archivo == null || !archivo.isFile())
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("path", path).setDetailMessage("No existe el archivo " + path);
		if ("true".equals(request().getQueryString("simular")))
			return json(com.bluedot.efactura.empresas.EmisoresElectronicos.cargarArchivo(archivo, true).toString());

		JSONObject resultado = com.bluedot.efactura.empresas.EmisoresElectronicos.ejecutar(usuario.getEmailAddress(), "Archivo " + archivo.getName(),
				new com.bluedot.efactura.empresas.EmisoresElectronicos.Operacion() {
					@Override
					public JSONObject ejecutar() throws APIException {
						return com.bluedot.efactura.empresas.EmisoresElectronicos.cargarArchivo(archivo, false);
					}
				});
		auditoria.info(usuario.getEmailAddress() + " cargo el archivo de emisores electronicos " + path + ": " + resultado.getString("resumen"));
		return json(resultado.toString());
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

		Tuple<List<Empresa>, Long> empresas = Empresa.find(texto, emisorElectronico, conFirma, ruts, page, pageSize,
				Orden.leer(request(), Empresa.ORDENES));

		JSONArray json = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresas.item1);

		return json(JSONUtils.createObjectList(json, empresas.item2, page, pageSize).toString());
	}


	public Promise<Result> crearEmpresa() throws APIException
	{
		JsonNode empresaJson = request().body().asJson();
		if (empresaJson == null)
			throw APIException.raise(APIErrors.BAD_JSON);
		// logoPath leia cualquier archivo del servidor y lo devolvia como logo: el logo va en base64, como en editarEmpresa
		if (empresaJson.has("logoPath"))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("logoPath")
					.setDetailMessage("logoPath ya no se acepta: mandar el logo en base64 en el campo logo");
		Empresa empresa = new Empresa();
		String rut = empresaJson.has("rut") ? empresaJson.findPath("rut").asText() : null;
		// RUT obligatorio, con el formato de DGI y unico (con dos empresas con el mismo RUT fallan todas las rutas /empresa/:rut)
		if (!FormatosDgi.esRuc(rut))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("rut", String.valueOf(rut))
					.setDetailMessage("El RUT tiene que tener 12 dígitos");
		if (Empresa.findByRUT(rut) != null)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("rut", rut)
					.setDetailMessage("Ya existe una empresa con ese RUT");
		String nombreComercial = empresaJson.has("nombreComercial") ? empresaJson.findPath("nombreComercial").asText() : null;
		String logo = texto(empresaJson, "logo");
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


		if (logo != null && !logo.isEmpty())
			empresa.setLogo(leerLogo(logo));

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

	/** Logo en base64 (con o sin prefijo data:...,): PNG o JPEG de hasta 512 KB */
	private static byte[] leerLogo(String logo) throws APIException {
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
		return imagen;
	}

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
		// Dada de baja (activa=false): los pollers no la procesan
		if (empresaJson.has("activa")) {
			JsonNode valor = empresaJson.get("activa");
			if (valor == null || !valor.isBoolean())
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("activa", String.valueOf(valor))
						.setDetailMessage("activa tiene que ser true o false");
			if (valor.asBoolean() != empresa.isActiva()) {
				empresa.setActiva(valor.asBoolean());
				cambios.add("activa");
			}
		}
		// Dias de espera de la respuesta a los CFE recibidos (ME_): null = el de la configuracion, -1 = nunca automaticamente
		if (empresaJson.has("diasRespuestaCfe")) {
			Integer dias = null;
			com.fasterxml.jackson.databind.JsonNode valor = empresaJson.get("diasRespuestaCfe");
			if (valor != null && !valor.isNull() && !valor.asText().trim().isEmpty()) {
				try {
					dias = Integer.valueOf(valor.asText().trim());
				} catch (NumberFormatException e) {
					dias = Integer.MIN_VALUE;
				}
				if (dias < -1 || dias > 90)
					throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("diasRespuestaCfe", valor.asText())
							.setDetailMessage("Los días de espera de la respuesta a los CFE tienen que ser de -1 (nunca automáticamente) a 90");
			}
			if (!Objects.equals(dias, empresa.getDiasRespuestaCfe())) {
				empresa.setDiasRespuestaCfe(dias);
				cambios.add("diasRespuestaCfe");
			}
		}

		// Logo: imagen en base64 (antes era logoPath, un archivo del servidor)
		String logo = texto(empresaJson, "logo");
		if (logo != null && !logo.isEmpty()) {
			empresa.setLogo(leerLogo(logo));
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
	
	/** Borra una sucursal sin comprobantes. Solo ADMIN. No se puede borrar la ultima sucursal de la empresa. */
	public Promise<Result> eliminarSucursal(String rut, Integer codigoSuc) throws APIException {
		User usuario = PermissionValidator.getSessionUser(ctx());
		AccesoEmpresa.verificarAdmin(usuario, "DELETE /empresa/" + rut + "/sucursales/" + codigoSuc, true);

		Empresa empresa = Empresa.findByRUT(rut, true);
		Sucursal sucursal = Sucursal.findByCodigoSucursal(empresa.getSucursales(), codigoSuc, true);

		if (empresa.getSucursales().size() <= 1)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CdgDGISucur", String.valueOf(codigoSuc))
					.setDetailMessage("No se puede borrar la única sucursal de la empresa");

		long comprobantes = CFE.countBySucursal(sucursal);
		if (comprobantes > 0)
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("CdgDGISucur", String.valueOf(codigoSuc))
					.setDetailMessage("La sucursal tiene " + comprobantes + " comprobante(s): no se puede borrar");

		// Primero se saca de la empresa: si quedara en la lista, su cascade la volveria a guardar
		empresa.getSucursales().remove(sucursal);
		sucursal.delete();

		auditoria.info(usuario.getEmailAddress() + " borró la sucursal " + codigoSuc + " (" + sucursal.getDomicilioFiscal()
				+ ") de la empresa " + rut);

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
