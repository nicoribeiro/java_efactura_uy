package com.bluedot.efactura.services;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.SobreRecibido;

import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;

public interface IntercambioService {

	ACKSobredefType procesarSobre(Empresa empresa, SobreRecibido sobreRecibido) throws APIException;
	
	ACKCFEdefType procesarCFESobre(Empresa empresa, SobreRecibido sobreRecibido) throws APIException;

}
