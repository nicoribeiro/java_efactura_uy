package com.bluedot.efactura.pollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Empresa;

public class EmailEntrantesPoller extends PollerRunner {
	
	private static final long SLEEP_TIME_IN_MILLIS = 60l * 1000l* 5l;
	
	static Logger logger = LoggerFactory.getLogger(EmailEntrantesPoller.class);

	public EmailEntrantesPoller() {
		super(logger,SLEEP_TIME_IN_MILLIS);
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		for (Empresa empresa : Empresa.findAll()) {//TODO mejorar esto, solo las que tengan para obtener mail no todas las empresas
			if (empresa.getHostRecepcion()!=null && empresa.getUserRecepcion()!=null && empresa.getPassRecepcion()!=null && empresa.getPuertoRecepcion()!=null){
				logger.debug("Obteniendo mensajes entrantes para Empresa id:{}, rut:{}, razon:{}", empresa.getId(), empresa.getRut(), empresa.getRazon());
				factory.getServiceMicroController(empresa).obtenerYProcesarEmailsEntrantesDesdeServerCorreo();
			}
		}
		
	}

	
}