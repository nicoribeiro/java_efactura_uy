package com.bluedot.efactura.controllers;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CAE;
import com.bluedot.efactura.model.Empresa;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@Api(value = "Operaciones de CAE") 
public class CAEController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(CAEController.class);

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
	
}
