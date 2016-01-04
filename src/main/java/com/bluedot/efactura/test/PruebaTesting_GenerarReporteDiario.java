package com.bluedot.efactura.test;

import java.util.Date;

import org.json.JSONException;

import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

import dgi.soap.recepcion.Data;

public class PruebaTesting_GenerarReporteDiario {

	public static void main(String[] args) {
		try {
			RecepcionService service = new RecepcionServiceImpl();
			
			Date date = new Date();
			
//			Date date = DateHandler.fromStringToDate("20151211", new SimpleDateFormat("yyyyMMdd"));
			
			Data data = service.generarReporteDiario(date);
			
			System.out.println("Output data:\n" + data.getXmlData());
		} catch (JSONException | EFacturaException e) {
			e.printStackTrace();
		}

	}

}
