package com.bluedot.efactura.controllers;

import org.json.JSONObject;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.serializers.EfacturaJSONSerializerProvider;
import com.play4jpa.jpa.db.Tx;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
@Api(value = "Operaciones de Pais") 
public class PaisesController extends AbstractController {
	
	
	@ApiOperation(value = "Encontrar un Pais por id",
		    notes = "Retorna un pais",
		    response = Pais.class
		    )
	
	public Promise<Result> getPais(int id) throws APIException {
		
		Pais pais = Pais.findById(id, true);
		
		JSONObject json = EfacturaJSONSerializerProvider.getPaisSerializer().objectToJson(pais);
		
		return json(json.toString());
	}
	

}
