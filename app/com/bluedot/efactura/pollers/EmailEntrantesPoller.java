package com.bluedot.efactura.pollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactory;
import com.bluedot.efactura.microControllers.factory.EfacturaMicroControllersFactoryBuilder;
import com.bluedot.efactura.model.Empresa;

public class EmailEntrantesPoller extends PollerRunner {
	
	private static final long SLEEP_TIME_IN_MILLIS = 60l * 1000l;
	
	static Logger logger = LoggerFactory.getLogger(EmailEntrantesPoller.class);

	public EmailEntrantesPoller() {
		super(logger,SLEEP_TIME_IN_MILLIS);
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		
		EfacturaMicroControllersFactory factory = (new EfacturaMicroControllersFactoryBuilder())
				.getMicroControllersFactory();
		
		for (Empresa empresa : Empresa.findAll()) {
			if (empresa.getHostRecepcion()!=null && empresa.getUserRecepcion()!=null && empresa.getPassRecepcion()!=null && empresa.getPuertoRecepcion()!=null){
				logger.info("Obteniendo mensajes entrantes para Empresa id:" + empresa.getId());
				factory.getServiceMicroController(empresa).getDocumentosEntrantes();
			}
		}
		
	}

	
}