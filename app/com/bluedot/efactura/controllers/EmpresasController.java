package com.bluedot.efactura.controllers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

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
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.FirmaDigital;
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
@Api(value = "Operaciones de Empresa") 
public class EmpresasController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(EmpresasController.class);

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

			File fXmlFile = new File(path);
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXmlFile);

			// optional, but recommended
			// read this -
			// http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
			doc.getDocumentElement().normalize();

			NodeList nList = doc.getElementsByTagName("RucEmisoresTransicionMail.RucEmisoresTransicionMailItem");

			for (int temp = 0; temp < nList.getLength(); temp++) {

				Node nNode = nList.item(temp);

				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					Element eElement = (Element) nNode;

					String rut = eElement.getElementsByTagName("RUC").item(0).getTextContent();

					logger.info("Procesando RUT: " + rut + " " + (temp + 1) + "/" + nList.getLength());

					String denominacion = eElement.getElementsByTagName("DENOMINACION").item(0).getTextContent();
					//String fechaInicio = eElement.getElementsByTagName("FECHA_INICIO").item(0).getTextContent();
					String mail = eElement.getElementsByTagName("MAIL").item(0).getTextContent();

					Empresa empresa = Empresa.findByRUT(rut);
					if ( empresa == null) {
						empresa = new Empresa(rut, null, null, null, null, null);
						empresa.save();
					}else{
						empresa.update();
					}
					
					empresa.setEmisorElectronico(true);
					empresa.setMailRecepcion(mail);
					empresa.setRazon(denominacion);

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
	
	public Promise<Result> getEmpresas() throws APIException {
		
		List<Empresa> empresas = Empresa.findAll();
		
		JSONArray json = EfacturaJSONSerializerProvider.getEmpresaSerializer().objectToJson(empresas);
		
		return json(json.toString());
	}
	
	//TODO agregar validacion de que el usuario tiene permisos sobre esta emepresa
	public Promise<Result> addCAE(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut,true);
		
		JsonNode jsonNode = request().body().asJson();

		JSONObject caeJson = new JSONObject(jsonNode.toString());
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		
		CAEMicroController caeMicroController = factory.getCAEMicroController(empresa);
		
		CAE cae = caeMicroController.getCAEfromJson(caeJson);
		
		caeMicroController.addCAE(cae);
		
		return json(OK);
	}
	
	//TODO permisis de edicion 
	public Promise<Result> editarEmpresa(String rut) throws APIException
	{
		Empresa empresa = Empresa.findByRUT(rut,true);
		
		JsonNode empresaJson = request().body().asJson();
		
		String departamento = empresaJson.has("departamento") ? empresaJson.findPath("departamento").asText() : null;
		String direccion = empresaJson.has("direccion") ? empresaJson.findPath("direccion").asText() : null;
		String nombreComercial = empresaJson.has("nombreComercial") ? empresaJson.findPath("nombreComercial").asText() : null;
		String localidad = empresaJson.has("localidad") ? empresaJson.findPath("localidad").asText() : null;
		Integer codigoSucursal = empresaJson.has("codigoSucursal") ? empresaJson.findPath("codigoSucursal").asInt() : null;
		String logoPath = empresaJson.has("logoPath") ? empresaJson.findPath("logoPath").asText() : null;
		String paginaWeb = empresaJson.has("paginaWeb") ? empresaJson.findPath("paginaWeb").asText() : null;
		String telefono = empresaJson.has("telefono") ? empresaJson.findPath("telefono").asText() : null;
		String codigoPostal = empresaJson.has("codigoPostal") ? empresaJson.findPath("codigoPostal").asText() : null;
		String resolucion = empresaJson.has("resolucion") ? empresaJson.findPath("resolucion").asText() : null;
		String razon = empresaJson.has("razon") ? empresaJson.findPath("razon").asText() : null;
		
		String hostRecepcion = empresaJson.has("hostRecepcion") ? empresaJson.findPath("hostRecepcion").asText() : null;
		String passRecepcion = empresaJson.has("passRecepcion") ? empresaJson.findPath("passRecepcion").asText() : null;
		String puertoRecepcion = empresaJson.has("puertoRecepcion") ? empresaJson.findPath("puertoRecepcion").asText() : null;
		String userRecepcion = empresaJson.has("userRecepcion") ? empresaJson.findPath("userRecepcion").asText() : null;
		String mailNotificaciones = empresaJson.has("mailNotificaciones") ? empresaJson.findPath("mailNotificaciones").asText() : null;
		String fromEnvio = empresaJson.has("fromEnvio") ? empresaJson.findPath("fromEnvio").asText() : null;
		
		String certificado = empresaJson.has("certificado") ? empresaJson.findPath("certificado").asText() : null;
		String privateKey = empresaJson.has("privateKey") ? empresaJson.findPath("privateKey").asText() : null;
		
		if (hostRecepcion != null)
			empresa.setHostRecepcion(hostRecepcion);
		
		if (passRecepcion != null)
			empresa.setPassRecepcion(passRecepcion);
		
		if (puertoRecepcion != null)
			empresa.setPuertoRecepcion(puertoRecepcion);
		
		if (userRecepcion != null)
			empresa.setUserRecepcion(userRecepcion);
		
		if (mailNotificaciones != null)
			empresa.setMailNotificaciones(mailNotificaciones);
		
		if (fromEnvio != null)
			empresa.setFromEnvio(fromEnvio);
		
		if (paginaWeb != null)
			empresa.setPaginaWeb(paginaWeb);
		
		if (telefono != null)
			empresa.setTelefono(telefono);
		
		if (codigoPostal != null)
			empresa.setCodigoPostal(codigoPostal);
		
		if (codigoSucursal != null)
			empresa.setCodigoSucursal(codigoSucursal);
		
		if (departamento != null)
			empresa.setDepartamento(departamento);
		
		if (direccion != null)
			empresa.setDireccion(direccion);
		
		if (nombreComercial != null)
			empresa.setNombreComercial(nombreComercial);
		
		if (localidad != null)
			empresa.setLocalidad(localidad);
		
		if (resolucion != null)
			empresa.setResolucion(resolucion);
		
		if (razon != null)
			empresa.setRazon(razon);
		
		if (logoPath!=null){
			Path path = Paths.get(logoPath);
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
		
		empresa.update();
		
		return json(OK);
	}

}
