package com.bluedot.efactura.main;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;

import org.json.JSONObject;

import com.bluedot.commons.IO;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.EFacturaFactory;
import com.bluedot.efactura.impl.EFacturaFactoryImpl;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.classes.recepcion.CFEDefType.EFact;
import dgi.soap.recepcion.Data;

public class CreateEfacturaTest
{

	public static void main(String[] args)
	{
		try
		{
			String file = IO.readFile("resources/json/efactura.json", Charset.defaultCharset());

			JSONObject json = new JSONObject(file);

			EFacturaFactory factory = new EFacturaFactoryImpl();

			/*
			 * Create Efact object from json description
			 */
			EFact efactura = factory.getCFEController().createEfactura(json.getJSONObject("eFact"));

			RecepcionService service = new RecepcionServiceImpl();

			// Call the service
			Data response = service.send(efactura);

			System.out.println("Output data:\n" + response.getXmlData());

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
		} catch (JAXBException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnrecoverableEntryException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatatypeConfigurationException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
