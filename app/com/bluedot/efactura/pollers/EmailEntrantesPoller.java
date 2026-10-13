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
		
		int empresas = 0, emails = 0;
		org.json.JSONArray porEmpresa = new org.json.JSONArray();
		java.util.List<String> resumen = new java.util.ArrayList<String>();
		for (Empresa empresa : Empresa.findEmpresasConMail()) {
			if (empresa.getHostRecepcion()!=null && empresa.getUserRecepcion()!=null && empresa.getPassRecepcion()!=null && empresa.getPuertoRecepcion()!=null){
				logger.debug("Obteniendo mensajes entrantes para Empresa id:{}, rut:{}, razon:{}", empresa.getId(), empresa.getRut(), empresa.getRazon());
				int nuevos = factory.getServiceMicroController(empresa).obtenerYProcesarEmailsEntrantesDesdeServerCorreo().size();
				empresas++;
				if (nuevos > 0) {
					emails += nuevos;
					resumen.add(empresa.getRut() + ": " + nuevos);
					porEmpresa.put(new org.json.JSONObject().put("rut", empresa.getRut()).put("emails", nuevos));
				}
			}
		}
		// Se registra solo si llego algo (corre cada pocos minutos)
		if (emails > 0)
			resultado(emails + " email(s) procesado(s) de " + empresas + " casilla(s) (" + String.join("; ", resumen) + ")",
					new org.json.JSONObject().put("casillas", empresas).put("emails", emails).put("empresas", porEmpresa));
		else
			estado.setResumen("Sin emails nuevos en " + empresas + " casilla(s)");
		
	}

	
}