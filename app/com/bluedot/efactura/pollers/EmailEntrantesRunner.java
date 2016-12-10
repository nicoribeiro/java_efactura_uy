package com.bluedot.efactura.pollers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroControllerFactory;
import com.bluedot.efactura.model.Empresa;

import play.db.jpa.JPAApi;

public class EmailEntrantesRunner extends PollerRunner {
	
	private static final long SLEEP_TIME_IN_MILLIS = 60l * 1000l * 10l;
	
	static Logger logger = LoggerFactory.getLogger(EmailEntrantesRunner.class);

	private JPAApi jpaApi;
	
	private ServiceMicroControllerFactory serviceMicroControllerFactory;

	
	public EmailEntrantesRunner(JPAApi api, ServiceMicroControllerFactory serviceMicroControllerFactory) {
		super(logger,SLEEP_TIME_IN_MILLIS);
		this.serviceMicroControllerFactory = serviceMicroControllerFactory;
	}

	@Override
	protected void executeConcreteAction() throws APIException {
		for (Empresa empresa : Empresa.findAll(jpaApi)) {
			if (empresa.getHostRecepcion()!=null && empresa.getUserRecepcion()!=null && empresa.getPassRecepcion()!=null && empresa.getPuertoRecepcion()!=null){
				logger.info("Obteniendo mensajes entrantes para Empresa id:" + empresa.getId());
				serviceMicroControllerFactory.create(empresa).getDocumentosEntrantes();
			}
		}
		
	}

	
}