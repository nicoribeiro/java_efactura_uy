package com.bluedot.efactura;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
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
	
	// Llama al web service real de DGI y la consulta se firma con una empresa con firma digital (de la base): no corre
	// como test unitario
	@Ignore("Necesita DGI y una empresa firmante")
	@Test
	public void test() throws Throwable
	{
		String response = service.getRutData("219000090011", null);
        Assert.assertTrue(response.contains("WS_PersonaActEmpresarial"));
	}

}
