package com.bluedot.efactura.controllers;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.pollers.PollerManager;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.google.inject.Inject;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@Api(value = "Operaciones de Sobres") 
public class SobresController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(SobresController.class);

	@Inject
	public SobresController(PollerManager pollerManager){
		super();
	}
	
	public Promise<Result> enviarSobreEmpresa(String rut, long sobreId) throws APIException {

		Sobre sobre = SobreEmitido.findById(sobreId,true);

		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		JSONObject error = null;
		
		if (sobre instanceof SobreEmitido)
			try {
				factory.getServiceMicroController(sobre.getEmpresaEmisora()).enviarCorreoReceptorElectronico((SobreEmitido)sobre);
			} catch (APIException e) {
				logger.error("APIException:", e);
				error = e.getJSONObject();
			}
		
		if (error == null)
			error =  new JSONObject(OK);

		return json(error.toString());

	}
	
	
	public Promise<Result> getSobre(String rut, long sobreId) throws APIException {
	
		Sobre sobre = SobreEmitido.findById(sobreId,true);

		JSONObject cfeJson;
		try {	
			cfeJson = EfacturaJSONSerializerProvider.getSobreSerializer().objectToJson(sobre);
		}catch (JSONException e) {
			throw APIException.raise(e);
		}
		
		return json(cfeJson.toString());
		
	}
	
	
	
	
}
