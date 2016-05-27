package com.bluedot.efactura;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.bluedot.efactura.services.ConsultaRutService;
import com.bluedot.efactura.services.impl.ConsultaRutServiceImpl;

public class CompanyDataServiceTest
{

	ConsultaRutService service;
	
	@Before
    public void setUp() {
        
        // Build a client (which is thread safe!)
        service = new ConsultaRutServiceImpl();
    }
	
	@Test
	public void test()
	{
		String response = service.getRutData("219000090011");
        Assert.assertTrue(response.contains("WS_PersonaActEmpresarial"));
	}

}
