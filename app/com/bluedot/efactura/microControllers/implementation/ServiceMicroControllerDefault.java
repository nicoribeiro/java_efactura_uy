package com.bluedot.efactura.microControllers.implementation;

import java.util.Date;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.strategy.builder.CFEBuiderInterface;
import com.bluedot.efactura.strategy.builder.CFEBuilderFactory;

import dgi.classes.respuestas.cfe.EstadoACKCFEType;
import dgi.classes.respuestas.sobre.EstadoACKSobreType;

public class ServiceMicroControllerDefault extends MicroControllerDefault implements ServiceMicroController {

	private RecepcionService recepcionService;
	private CAEMicroController caeMicroController;

	public ServiceMicroControllerDefault(RecepcionService recepcionService, Empresa empresa, CAEMicroController caeMicroController) {
		super(empresa);
		this.recepcionService = recepcionService;
		this.caeMicroController = caeMicroController;
	}	

	@Override
	public void enviar(CFE cfe) throws APIException {

		CFEBuiderInterface builder = CFEBuilderFactory.getCFEBuilder(cfe, caeMicroController);
		//TODO mutex
		builder.asignarId();
		
		recepcionService.sendCFE(cfe);

	}
	
	@Override
	public void reenviar(SobreEmitido sobre) throws APIException {
		recepcionService.reenviarSobre(sobre);
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

	@Override
	public void anularDocumento(CFE cfe) throws APIException {
		boolean anular = false;
		/*
		 * El sobre fue rechazado
		 */
		if (cfe.getSobre()!=null && cfe.getSobre().getEstado() != null && cfe.getSobre().getEstado()== EstadoACKSobreType.BS)
			anular = true;
			
		/*
		 * El cfe fue rechazado
		 */
		if (cfe.getEstado() != null && cfe.getEstado() == EstadoACKCFEType.BE)
			anular = true;
		
		
		if (anular){
			if (cfe.getGeneradorId()==null){
				throw APIException.raise(APIErrors.CFE_YA_FUE_ANULADO);
			}else{
				cfe.setGeneradorId(null);
				cfe.update();
			}
		}else
			throw APIException.raise(APIErrors.CFE_NO_SE_PUEDE_ANULAR);
		
		
	}

	

}
