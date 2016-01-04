package com.bluedot.efactura.main;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.soap.recepcion.Data;

public class ConsultaEstadoEnvioSobre
{

	public static void main(String[] args)
	{
		try
		{
			
			RecepcionService service = new RecepcionServiceImpl();

			String token = "R1fOh9ztUFqkGRr1pDoEWVgq+RIe5Wcw+3gMyfJwgOcJu5cUdI63im6wfUudnVvSAsw5nswB4hVXeB6E+PM1m03t3bZn+GmKsZRCo1h9zr2PqaqWOGmteQ44bbmHLnuA";
			
			String idReceptor = "28562586";
			
			// Call the service
			Data response = service.consultaEstado(token, idReceptor);

			System.out.println("Output data:\n" + response.getXmlData());

		} catch (EFacturaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
