package com.bluedot.efactura.services;

import dgi.classes.entreEmpresas.EnvioCFEEntreEmpresas;
import dgi.classes.respuestas.cfe.ACKCFEdefType;
import dgi.classes.respuestas.sobre.ACKSobredefType;

public interface IntercambioService {

	ACKSobredefType procesarSobre(EnvioCFEEntreEmpresas envioCFEEntreEmpresas, String filename);
	
	ACKCFEdefType procesarCFESobre(EnvioCFEEntreEmpresas envioCFEEntreEmpresas, ACKSobredefType ackSobredefType, String filename);

}
