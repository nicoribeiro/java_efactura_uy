package com.bluedot.efactura.pollers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.bluedot.efactura.model.Empresa;

public class NotificationPollerTest {

	private static Empresa completa() {
		Empresa empresa = new Empresa();
		empresa.setHostRecepcion("smtp.example.com");
		empresa.setPuertoRecepcion("465");
		empresa.setUserRecepcion("usuario");
		empresa.setPassRecepcion("x");
		empresa.setFromEnvio("efactura@example.com");
		empresa.setMailNotificaciones("avisos@example.com");
		return empresa;
	}

	@Test
	public void configuracionCompleta() {
		assertNull(NotificationPoller.faltaConfiguracionCorreo(completa()));
	}

	/** Como Lerand Group en prod el 2026-10-14: sin host (antes NullPointerException en SendMail) */
	@Test
	public void sinHost() {
		Empresa empresa = completa();
		empresa.setHostRecepcion(null);
		assertEquals("host", NotificationPoller.faltaConfiguracionCorreo(empresa));
	}

	@Test
	public void variosFaltantes() {
		Empresa empresa = completa();
		empresa.setPuertoRecepcion("abc");
		empresa.setFromEnvio(" ");
		empresa.setMailNotificaciones(null);
		assertEquals("puerto válido, remitente, mail de notificaciones", NotificationPoller.faltaConfiguracionCorreo(empresa));
	}

	@Test
	public void activaPorDefecto() {
		Empresa empresa = new Empresa();
		assertEquals(true, empresa.isActiva());
		empresa.setActiva(false);
		assertEquals(false, empresa.isActiva());
	}
}
