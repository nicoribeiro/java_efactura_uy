package com.bluedot.efactura.services.impl;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

import javax.inject.Inject;

import com.bluedot.commons.error.APIException;
import com.bluedot.efactura.pool.WSPersonaGetActEmpresarialSoapPortWrapper;
import com.bluedot.efactura.pool.WSRutPool;
import com.bluedot.efactura.services.ConsultaRutService;

import dgi.soap.rut.WSPersonaGetActEmpresarialExecute;
import dgi.soap.rut.WSPersonaGetActEmpresarialExecuteResponse;
import play.Application;

public class ConsultaRutServiceImpl implements ConsultaRutService {

	private WSRutPool wsRutPool;

	@Inject
	public ConsultaRutServiceImpl(WSRutPool wsRutPool) {
		this.wsRutPool = wsRutPool;
	}

	@Override
	public String getRutData(String rut) {

		Objects.requireNonNull(rut, "Parameter RUT is required");

		WSPersonaGetActEmpresarialSoapPortWrapper portWrapper = wsRutPool.checkOut();

		WSPersonaGetActEmpresarialExecute input = new WSPersonaGetActEmpresarialExecute();
		input.setRut(rut);

		WSPersonaGetActEmpresarialExecuteResponse output = portWrapper.getPort().execute(input);

		wsRutPool.checkIn(portWrapper);

		return output.getData();

	}
}
