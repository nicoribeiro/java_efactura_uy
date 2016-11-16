package com.bluedot.commons.utils;

import java.util.Base64;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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
import javax.mail.internet.PreencodedMimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SendMail
{

	final static Logger logger = LoggerFactory.getLogger(SendMail.class);

	public static void sendMail(String username, String password, String smtpHost, int smtpPort, String from, String recipient, String subject, String textVersion, String htmlVersion, Map<String, String> attachments, boolean logInstedOfSend) throws MessagingException
	{
		Address[] array = new Address[1];

		array[0] = new InternetAddress(recipient);
		sendMail(username, password, smtpHost, smtpPort, from, array, subject, textVersion, htmlVersion, attachments, logInstedOfSend);

	}

	public static void sendMail(String username, String password, String smtpHost, int smtpPort, String from, Address[] recipients, String subject, String textVersion, String htmlVersion, Map<String, String> attachments, boolean logInstedOfSend) throws AddressException,
			MessagingException
	{

		final String usernameFinal = username;

		final String passwordFinal = password;

		// Get system properties
		Properties properties = new Properties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", smtpHost);
		properties.setProperty("mail.smtp.auth", "true");
		properties.put("mail.transport.protocol", "smtp");
		properties.put("mail.smtp.timeout", "10000");
		properties.put("mail.smtp.socketFactory.port", String.valueOf(smtpPort));
		properties.put("mail.smtp.ssl.enable", "true");
		properties.put("mail.smtp.socketFactory.fallback", "false");
		properties.put("mail.smtp.ssl.checkserveridentity", "false");
		properties.put("mail.smtp.connectiontimeout", "10000");
		properties.put("mail.smtp.debug", "true");
		
		// Get the default Session object.
		Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication()
			{
				return new PasswordAuthentication(usernameFinal, passwordFinal);
			}
		});

		// Create a default MimeMessage object.
		MimeMessage message = new MimeMessage(session);
		Multipart multiPart;
		if (attachments.size()>0)
			multiPart = new MimeMultipart("mixed");
		else
			multiPart = new MimeMultipart("alternative");

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
//			try {
				for (Iterator<String> iterator = attachments.keySet().iterator(); iterator.hasNext();) {
					String filename = iterator.next();
					MimeBodyPart attachmnetPart = new PreencodedMimeBodyPart("base64");
//					DataSource source = new ByteArrayDataSource(attachments.get(filename), "application/octet-stream");
//					attachmnetPart.setDataHandler(new DataHandler(source));
					attachmnetPart.setContent(Base64.getEncoder().encodeToString(attachments.get(filename).getBytes()), "application/octet-stream");  
					attachmnetPart.setFileName(filename);
					multiPart.addBodyPart(attachmnetPart);
				}
//			} catch (IOException e) {
				/*TODO usar esto para loggear Exceptions: 
				* logger.error("Error", e);
				* 
				* Esto aplica para todo el proyecto no solo para este metodo
				*/
//				e.printStackTrace();
//			}
		
		message.setContent(multiPart);
		message.saveChanges();
		
		if (!logInstedOfSend)
		{
			// Send message
			Transport transport = session.getTransport("smtp");
			transport.send(message);
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
