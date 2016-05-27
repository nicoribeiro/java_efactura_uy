package com.bluedot.efactura.controllers;

import com.bluedot.efactura.EFacturaFactory.MODO;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.global.EFacturaException.EFacturaErrors;
import com.bluedot.efactura.global.ErrorMessage;
import com.bluedot.efactura.global.Secured;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.ConsultaRutService;
import com.bluedot.efactura.services.impl.ConsultaRutServiceImpl;

import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;

@ErrorMessage
@Security.Authenticated(Secured.class)
public class RutController extends Controller {

	public Result darInformacionRut(String idrut) throws EFacturaException {
		ConsultaRutService service = new ConsultaRutServiceImpl();

		// Call the service
		String response = service.getRutData(idrut);

		return ok(response).as("application/json");
	}

}
