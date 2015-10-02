package com.bluedot.efactura.main;

import java.io.IOException;
import java.nio.charset.Charset;

import com.bluedot.commons.IO;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.soap.recepcion.Data;


public class EnvioDeCFETest {

    public static void main(String[] args) {

        RecepcionService service = new RecepcionServiceImpl();
     
        // Call the service
        Data response;
		try
		{
			
			response = service.send(IO.readFile("resources/examples/cfe-signed.xml", Charset.forName("UTF-8")));
			
			 System.out.println("Output data:\n" + response.getXmlData());
		} catch (Throwable e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

        
       
    }
}
