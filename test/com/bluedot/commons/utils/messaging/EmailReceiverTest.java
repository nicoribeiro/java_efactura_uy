package com.bluedot.commons.utils.messaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Properties;

import org.junit.Test;

public class EmailReceiverTest {

	/** IMAP con SSL: JavaMail verifica el certificado y que sea del servidor (antes no se chequeaba el nombre) */
	@Test
	public void imapSslVerificaElServidor() {
		Properties p = EmailReceiver.getServerProperties("imap", "mail.ejemplo.com.uy", "993", true);
		assertEquals("true", p.getProperty("mail.imap.ssl.enable"));
		assertEquals("true", p.getProperty("mail.imap.ssl.checkserveridentity"));
		assertEquals("993", p.get("mail.imap.port"));
		assertNull(p.getProperty("mail.imap.socketFactory.class"));
	}

	@Test
	public void sinSsl() {
		Properties p = EmailReceiver.getServerProperties("imap", "mail.ejemplo.com.uy", "143", false);
		assertNull(p.getProperty("mail.imap.ssl.enable"));
	}
}
