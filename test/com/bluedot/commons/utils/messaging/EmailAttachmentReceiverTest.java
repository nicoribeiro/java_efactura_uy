package com.bluedot.commons.utils.messaging;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.mail.internet.MimeBodyPart;

import org.junit.Test;

public class EmailAttachmentReceiverTest {

	private static MimeBodyPart parte(String contenido, String tipo, String nombre) throws Exception {
		MimeBodyPart parte = new MimeBodyPart();
		parte.setContent(contenido, tipo);
		// Como llega del servidor: el Content-Type en los encabezados
		parte.setHeader("Content-Type", tipo);
		if (nombre != null)
			parte.setFileName(nombre);
		return parte;
	}

	/** Las notificaciones de Google traen una version AMP del cuerpo (email 36872 en prod) */
	@Test
	public void ampSinNombreEsCuerpo() throws Exception {
		assertTrue(EmailAttachmentReceiver.esCuerpoAlternativo(parte("<html amp4email></html>", "text/x-amp-html; charset=utf-8", null)));
		assertTrue(EmailAttachmentReceiver.esCuerpoAlternativo(parte("BEGIN:VCALENDAR", "text/calendar", null)));
	}

	@Test
	public void conNombreEsAdjunto() throws Exception {
		assertFalse(EmailAttachmentReceiver.esCuerpoAlternativo(parte("a;b", "text/csv", "datos.csv")));
	}

	@Test
	public void xmlSinNombreSigueSiendoAdjunto() throws Exception {
		assertFalse(EmailAttachmentReceiver.esCuerpoAlternativo(parte("<EnvioCFE_entreEmpresas/>", "text/xml", null)));
	}

	@Test
	public void binarioNoEsCuerpo() throws Exception {
		assertFalse(EmailAttachmentReceiver.esCuerpoAlternativo(parte("x", "application/octet-stream", null)));
	}
}
