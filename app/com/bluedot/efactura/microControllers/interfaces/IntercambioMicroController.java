package com.bluedot.efactura.microControllers.interfaces;

import org.w3c.dom.Document;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.SobreRecibido;

import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;

public interface IntercambioMicroController {

	ACKSobredefType procesarSobre(Empresa empresa, SobreRecibido sobreRecibido, Document documentCrudo) throws APIException;
	
	ACKCFEdefType procesarCFESobre(Empresa empresa, SobreRecibido sobreRecibido) throws APIException;

}
