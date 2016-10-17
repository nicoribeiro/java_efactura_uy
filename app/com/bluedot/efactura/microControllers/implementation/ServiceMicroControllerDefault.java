package com.bluedot.efactura.microControllers.implementation;

import java.util.Date;
import java.util.List;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.Sobre;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

public class ServiceMicroControllerDefault extends MicroControllerDefault implements ServiceMicroController {

	private RecepcionService recepcionService;
	private CAEMicroController caeMicroController;

	public ServiceMicroControllerDefault(RecepcionService recepcionService, Empresa empresa, CAEMicroController caeMicroController) {
		super(empresa);
		this.recepcionService = recepcionService;
		this.caeMicroController = caeMicroController;
	}

	@Override
	public void register(CFE cfe, String adenda) throws APIException {

		CFEBuiderInterface builder = CFEBuilderFactory.getCFEBuilder(cfe, caeMicroController);
		//TODO mutex
		builder.asignarId();
		
		recepcionService.sendCFE(cfe, adenda);

	}

	@Override
	public void consultarResultados(Date date) throws APIException {
		recepcionService.consultarResultados(date, empresa);
	}

	@Override
	public SobreEmitido consultaResultado(SobreEmitido sobre) throws APIException {
		recepcionService.consultaResultadoSobre(sobre);
		return sobre;
	}

	@Override
	public ReporteDiario generarReporteDiario(Date date, Empresa empresa) throws APIException {
		return recepcionService.generarReporteDiario(date, empresa);
	}

}
