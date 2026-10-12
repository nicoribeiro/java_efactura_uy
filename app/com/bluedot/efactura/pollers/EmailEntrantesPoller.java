package com.bluedot.efactura.pollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Empresa;

public class EmailEntrantesPoller extends PollerRunner {
	
	
	static Logger logger = LoggerFactory.getLogger(EmailEntrantesPoller.class);

	public EmailEntrantesPoller(play.Configuration config) {
		super(logger, intervaloConfig(config, "EmailEntrantesPoller", 5));
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		for (Empresa empresa : Empresa.findEmpresasConMail()) {
			if (empresa.getHostRecepcion()!=null && empresa.getUserRecepcion()!=null && empresa.getPassRecepcion()!=null && empresa.getPuertoRecepcion()!=null){
				logger.debug("Obteniendo mensajes entrantes para Empresa id:{}, rut:{}, razon:{}", empresa.getId(), empresa.getRut(), empresa.getRazon());
				factory.getServiceMicroController(empresa).obtenerYProcesarEmailsEntrantesDesdeServerCorreo();
			}
		}
		
	}

	
}