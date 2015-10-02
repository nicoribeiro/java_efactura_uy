package com.bluedot.efactura.services.impl;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.pool.WSRutPool;
import com.bluedot.efactura.services.ConsultaRutService;

import dgi.soap.rut.WSPersonaGetActEmpresarialExecute;
import dgi.soap.rut.WSPersonaGetActEmpresarialExecuteResponse;
import dgi.soap.rut.WSPersonaGetActEmpresarialSoapPort;

public class ConsultaRutServiceImpl implements ConsultaRutService {

    @Override
    public String getRutData(String rut) {
        try
		{
			Objects.requireNonNull(rut, "Parameter RUT is required");

			WSPersonaGetActEmpresarialSoapPort port = WSRutPool.getInstance().checkOut();

			WSPersonaGetActEmpresarialExecute input = new WSPersonaGetActEmpresarialExecute();
			input.setRut(rut);
			
			WSPersonaGetActEmpresarialExecuteResponse output = port.execute(input);
			
			WSRutPool.getInstance().checkIn(port);

			return output.getData();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (EFacturaException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (KeyStoreException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CertificateException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return null;
    }
}
