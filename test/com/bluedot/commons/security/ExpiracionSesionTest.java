package com.bluedot.commons.security;

import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;

public class ExpiracionSesionTest {

	/** Un token viejo sin fecha de creacion ni de ultimo uso no puede quedar vigente para siempre */
	@Test
	public void sesionSinFechasEstaVencida() {
		Session session = new Session();
		session.setCreationTimestamp(null);
		session.setLastAccess(null);
		assertNotNull(ExpiracionSesion.motivoVencimiento(session, new Date()));
	}
}
