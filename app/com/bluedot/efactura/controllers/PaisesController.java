package com.bluedot.efactura.controllers;

import java.util.concurrent.CompletionStage;

import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.VerboseAction;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.play4jpa.jpa.db.Tx;

import play.Application;
import play.db.jpa.JPAApi;
import play.db.jpa.Transactional;
import play.mvc.Result;
import play.mvc.Security;
import play.mvc.With;

@With(VerboseAction.class)
@Tx
@Transactional
@Security.Authenticated(Secured.class)
public class PaisesController extends AbstractController {
	
	@Inject
	public PaisesController(JPAApi jpaApi, Provider<Application> application) {
		super(jpaApi, application);
	}

	public CompletionStage<Result> getPais(int id) throws APIException {
		
		Pais pais = Pais.findById(jpaApi, id, true);
		
		JSONObject json = EfacturaJSONSerializerProvider.getPaisSerializer().objectToJson(jpaApi, pais);
		
		return json(json.toString());
	}
	

}
