package com.bluedot.efactura.services;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.model.Empresa;

public interface ConsultaRutService {

	/**
	 * Datos del RUT en DGI (web service personaGetActEmpresarial). La consulta se firma con el certificado de la empresa
	 * firmante.
	 */
	public String getRutData(String rut, Empresa firmante) throws APIException;

}
