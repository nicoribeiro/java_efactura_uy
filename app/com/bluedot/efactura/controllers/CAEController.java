package com.bluedot.efactura.controllers;

import com.bluedot.efactura.security.AccesoEmpresa;
import com.bluedot.efactura.security.EmpresaAccess;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.PermissionValidator;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@EmpresaAccess
@Api(value = "Operaciones de CAE") 
public class CAEController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(CAEController.class);

	final static Logger auditoria = LoggerFactory.getLogger("auditoria");

	/**
	 * CAEs de la empresa (todos, incluidos vencidos y anulados), con su uso
	 */
	//TODO agregar validacion de que el usuario tiene permisos sobre esta emepresa
	public Promise<Result> getCAEs(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut, true);

		JSONArray json = EfacturaJSONSerializerProvider.getCAESerializer().objectToJson(CAE.findByEmpresa(empresa));

		return json(json.toString());
	}

	//TODO agregar validacion de que el usuario tiene permisos sobre esta emepresa
	public Promise<Result> addCAE(String rut) throws APIException {
		Empresa empresa = Empresa.findByRUT(rut,true);
		
		JsonNode jsonNode = request().body().asJson();

		JSONObject caeJson = new JSONObject(jsonNode.toString());

		// Cargar CAE: solo ADMIN, en cualquier modo de seguridad.accesoEmpresa
		AccesoEmpresa.verificarAdmin(PermissionValidator.getSessionUser(ctx()), "POST /empresa/" + rut + "/cae", true);

		// El CAE de DGI trae el RUT del emisor (RucE): tiene que ser el de la empresa
		if (caeJson.has("RucE") && !rut.equals(caeJson.getString("RucE").trim()))
			throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("RucE", caeJson.getString("RucE"))
					.setDetailMessage("El CAE es de otra empresa (RUT " + caeJson.getString("RucE") + ")");
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder()).getMicroControllersFactory();
		
		CAEMicroController caeMicroController = factory.getCAEMicroController(empresa);
		
		CAE cae = caeMicroController.getCAEfromJson(caeJson);

		// Sin duplicados ni rangos superpuestos del mismo tipo y serie (numerarian dos comprobantes iguales)
		for (CAE existente : CAE.findByEmpresa(empresa)) {
			if (existente.getNro() == cae.getNro())
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("NA", cae.getNro())
						.setDetailMessage("El CAE " + cae.getNro() + " ya está cargado");
			if (existente.getTipo() == cae.getTipo() && existente.getSerie() != null && existente.getSerie().equals(cae.getSerie())
					&& cae.getInicial() <= existente.getFin() && existente.getInicial() <= cae.getFin())
				throw APIException.raise(APIErrors.BAD_PARAMETER_VALUE).withParams("DNro-HNro", cae.getInicial() + "-" + cae.getFin())
						.setDetailMessage("El rango " + cae.getInicial() + "-" + cae.getFin() + " se superpone con el CAE "
								+ existente.getNro() + " (" + existente.getInicial() + "-" + existente.getFin() + ")");
		}
		
		caeMicroController.addCAE(cae);

		auditoria.info(PermissionValidator.getSessionUser(ctx()).getEmailAddress() + " cargó el CAE " + cae.getNro() + " ("
				+ cae.getTipo() + " serie " + cae.getSerie() + ", " + cae.getInicial() + "-" + cae.getFin() + ") de la empresa " + rut);
		
		return json(OK);
	}
	
}
