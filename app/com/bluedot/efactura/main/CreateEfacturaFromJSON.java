package com.bluedot.efactura.main;

import java.io.IOException;
import java.nio.charset.Charset;

import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.global.EFacturaException;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.soap.recepcion.Data;

public class CreateEfacturaFromJSON
{

	public static void main(String[] args)
	{
		try
		{
			String file = IO.readFile("resources/json/efactura.json", Charset.defaultCharset());

			JSONObject json = new JSONObject(file);

			EFacturaFactory factory = EFacturaFactoryImpl.getInstance();

			/*
			 * Create Efact object from json description
			 */
			EFact efactura = factory.getCFEController().createEfactura(json.getJSONObject("eFact"));

			RecepcionService service = new RecepcionServiceImpl();

			// Call the service
			Data response = service.sendCFE(efactura);

			System.out.println("Output data:\n" + response.getXmlData());

		
		} catch (EFacturaException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

}
