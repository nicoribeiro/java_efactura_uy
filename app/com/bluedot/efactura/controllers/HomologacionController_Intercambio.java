package com.bluedot.efactura.controllers;

import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.ErrorMessage;
import com.bluedot.efactura.global.Secured;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;

import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class HomologacionController_Intercambio extends Controller {

	protected HomologacionController_Intercambio() {

	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result ingresoSobre() throws EFacturaException {
		try {

			JsonNode jsonNode = request().body().asJson();

			return ok("").as("application/json");

		} catch (JSONException e) {
			throw EFacturaException.raise(e);
		}

	}
}
