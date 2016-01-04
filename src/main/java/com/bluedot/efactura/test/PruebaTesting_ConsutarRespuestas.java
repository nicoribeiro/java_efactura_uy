package com.bluedot.efactura.test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.bluedot.commons.DateHandler;
import com.bluedot.efactura.EFacturaException;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.RecepcionService.ResultadoConsulta;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;

public class PruebaTesting_ConsutarRespuestas {

	public static void main(String[] args) {
		RecepcionService service = new RecepcionServiceImpl();
		try {
			
			Date date = DateHandler.fromStringToDate("20151222", new SimpleDateFormat("yyyyMMdd"));
			
			List<ResultadoConsulta> result = service.consultarResultados(date);
			
			for (Iterator<ResultadoConsulta> iterator = result.iterator(); iterator.hasNext();) {
				ResultadoConsulta resultadoConsulta =  iterator.next();
				System.out.println(resultadoConsulta.toString());
			}
			
			
		} catch (EFacturaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
