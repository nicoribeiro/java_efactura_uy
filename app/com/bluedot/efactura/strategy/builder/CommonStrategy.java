package com.bluedot.efactura.strategy.builder;

import java.util.Calendar;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Pais;
import com.bluedot.efactura.model.TipoDocumento;
import com.bluedot.efactura.model.Titular;
import com.bluedot.efactura.model.UI;

import dgi.classes.recepcion.TipMonType;

public class CommonStrategy {

	protected CFE cfe;
	protected CAEMicroController caeMicroController;

	public CommonStrategy(CFE cfe, CAEMicroController caeMicroController) throws APIException {
		this.caeMicroController = caeMicroController;
		this.cfe = cfe;
	}

	protected boolean supera10000UI() throws APIException {

		/*
		 * Sumatoria de campos 112 a 118 segun documentacion
		 */
		double monto = cfe.getTotMntNoGrv() + cfe.getTotMntExpyAsim() + cfe.getTotMntImpPerc()
				+ cfe.getTotMntIVAenSusp() + cfe.getTotMntIVATasaMin() + cfe.getTotMntIVATasaBas()
				+ cfe.getTotMntIVAOtra();

		if (cfe.getMoneda()!=TipMonType.UYU)
			monto = monto*cfe.getTipoCambio();
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(cfe.getFecha());
		int anio = cal.get(Calendar.YEAR);

		UI ui = UI.findByAnio(anio, true);

		return (monto / ui.getCotizacion()) > 10000;

	}

	public Titular getOrCreateTitular(Pais paisEmisorDocumento, TipoDocumento tipoDocumento, String documento)
			throws APIException {

		Titular titular = Titular.findById(paisEmisorDocumento, tipoDocumento, documento);
		/*
		 * Si el titular no existe la registro como nuevo en el sistema
		 */
		if (titular == null) {
			titular = new Titular(paisEmisorDocumento, tipoDocumento, documento);
			titular.save();
		}

		return titular;

	}

	public Empresa getOrCreateEmpresa(String docRecep, String rznSocRecep, String dirRecep, String ciudadRecep,
			String deptoRecep) {
		Empresa empresa = Empresa.findByRUT(docRecep);
		
		if (empresa == null) {
			/*
			 * Si la empresa no existe la registro como nueva en el sistema
			 */
			empresa = new Empresa(docRecep, rznSocRecep, null, dirRecep, ciudadRecep, deptoRecep, 0, null);
			empresa.save();
		}else{
			/*
			 * Si la empresa existe veo si puedo actualizar algun dato
			 */
			if (empresa.getDepartamento()==null)
				empresa.setDepartamento(deptoRecep);
			if (empresa.getLocalidad()==null)
				empresa.setLocalidad(ciudadRecep);
			if (empresa.getDireccion()==null)
				empresa.setDireccion(dirRecep);
			if (empresa.getRazon()==null)
				empresa.setRazon(rznSocRecep);
			empresa.update();
		}
			
		
		return empresa;
	}
}
