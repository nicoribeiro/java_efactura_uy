package com.bluedot.commons.utils;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SendMail
{

	final static Logger logger = LoggerFactory.getLogger(SendMail.class);

	public static void sendMail(String username, String password, String smtpHost, String smtpPort, String from, String recipient, String subject, String textVersion, String htmlVersion, Map<String, String> attachments, boolean logInstedOfSend) throws MessagingException
	{
		Address[] array = new Address[1];

		array[0] = new InternetAddress(recipient);
		sendMail(username, password, smtpHost, smtpPort, from, array, subject, textVersion, htmlVersion, attachments, logInstedOfSend);

	}

	public static void sendMail(String username, String password, String smtpHost, String smtpPort, String from, Address[] recipients, String subject, String textVersion, String htmlVersion, Map<String, String> attachments, boolean logInstedOfSend) throws AddressException,
			MessagingException
	{

		final String usernameFinal = username;

		final String passwordFinal = password;

		// Get system properties
		Properties properties = System.getProperties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", smtpHost);
		properties.setProperty("mail.smtp.auth", "true");
		properties.setProperty("mail.smtp.starttls.enable", "true");
		
		properties.put("mail.smtp.socketFactory.port", smtpPort);
		properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		properties.put("mail.smtp.socketFactory.fallback", "false");

		// Get the default Session object.
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(usernameFinal, passwordFinal);
			}
		});

		// session.setDebug(true);

		// Create a default MimeMessage object.
		MimeMessage message = new MimeMessage(session);
		Multipart multiPart = new MimeMultipart("alternative");

		// Set From: header field of the header.
		message.setFrom(new InternetAddress(from));

		// Set To: header field of the header.
		message.addRecipients(Message.RecipientType.TO, recipients);

		// Set Subject: header field
		message.setSubject(subject);

		// Now set the actual message
		if(textVersion != null){
			MimeBodyPart textPart = new MimeBodyPart();
		    textPart.setText(textVersion, "utf-8");
		    multiPart.addBodyPart(textPart);
		}
		if(htmlVersion != null){
			 MimeBodyPart htmlPart = new MimeBodyPart();
			 htmlPart.setContent(htmlVersion, "text/html; charset=utf-8");
			 multiPart.addBodyPart(htmlPart);
		}
		
		// Add attachments if any
		if (attachments!=null)
			try {
				for (Iterator<String> iterator = attachments.keySet().iterator(); iterator.hasNext();) {
					String filename = iterator.next();
					MimeBodyPart attachmnetPart = new MimeBodyPart();
					//DataSource source = new FileDataSource(filename);
					DataSource source = new ByteArrayDataSource(attachments.get(filename), "application/octet-stream");
					attachmnetPart.setDataHandler(new DataHandler(source));
					attachmnetPart.setFileName(filename);
					multiPart.addBodyPart(attachmnetPart);
				}
			} catch (IOException e) {
				/*TODO usar esto para loggear Exceptions: 
				* logger.error("Error", e);
				* 
				* Esto aplica para todo el proyecto no solo para este metodo
				*/
				e.printStackTrace();
			}
		
		message.setContent(multiPart);
		
		if (!logInstedOfSend)
		{
			// Send message
			Transport.send(message);
		}
		StringBuilder mails = new StringBuilder();

		for (Address address : recipients)
		{
			mails.append(address.toString() + " ");
		}

		if (logInstedOfSend)
		{
			logger.info("HOST: " + smtpHost);
			logger.info("FROM: " + from);
			logger.info("SUBJECT: " + subject);
			logger.info("TO: " + mails);
			logger.info("BODY-TEXT: " + textVersion);
			logger.info("BODY-HTML: " + htmlVersion);
		} else
			logger.info("Sent message successfully to: " + mails);

	}

}
