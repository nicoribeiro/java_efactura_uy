package com.bluedot.efactura.microControllers.interfaces;

import java.util.Date;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.ReporteDiario;
import com.bluedot.efactura.model.SobreEmitido;

public interface ServiceMicroController {

	void register(CFE eFactura, String adenda) throws APIException;

	void consultarResultados(Date date) throws APIException;

	SobreEmitido consultaResultado(SobreEmitido sobre) throws APIException;

	ReporteDiario generarReporteDiario(Date date, Empresa empresa) throws APIException;

}
