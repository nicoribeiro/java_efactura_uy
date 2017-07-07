package com.bluedot.commons.utils.messaging;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPMessage;

/**
 * This program demonstrates how to download e-mail messages
 *
 * @author www.codejava.net
 * @author nicolasribeiro
 *
 */
public class EmailAttachmentReceiver {
	final static Logger logger = LoggerFactory.getLogger(EmailAttachmentReceiver.class);

	/**
	 * Downloads new messages and saves attachments to disk if any.
	 * 
	 * @param protocol
	 *            imap or pop3
	 * @param host
	 * @param port
	 * @param userName
	 * @param password
	 */
	public List<Email> downloadEmail(String protocol, String host, String port, String userName, String password,
			int offset, int messageQuantity) {

		/*
		 * Port 110 - this is the default POP3 non-encrypted port Port 995 - this is the
		 * port you need to use if you want to connect using POP3 securely
		 * 
		 * Port 143 - this is the default IMAP non-encrypted port Port 993 - this is the
		 * port you need to use if you want to connect using IMAP securely
		 * 
		 * Port 25 - this is the default SMTP non-encrypted port Port 465 - this is the
		 * port used, if you want to send messages using SMTP securely
		 */

		List<Email> emails = new LinkedList<Email>();

		Properties properties = EmailReceiver.getServerProperties(protocol, host, port, true);

		Session session = Session.getInstance(properties);

		try {
			String packageName = "javax.mail.internet.";
			String simpleClassName = "MimeMultipart";
			String className = packageName + simpleClassName;
			Class<?> cl = Class.forName(className);
			URL url = cl.getResource(simpleClassName + ".class");
			logger.debug("url=" + url);
		
			// connects to the message store
			Store store = session.getStore(protocol);
			store.connect(userName, password);

			// opens the inbox folder
			Folder folderInbox = store.getFolder("INBOX");
			folderInbox.open(Folder.READ_ONLY);
			int messageCount = folderInbox.getMessageCount();
			int end = offset + messageQuantity - 1;

			if (offset < 1)
				offset = 1;

			if (end > messageCount)
				end = messageCount;

			// fetches new messages from server
			Message[] arrayMessages = folderInbox.getMessages(offset, end);

			/* Get the messages which is unread in the Inbox */
			// Message messages[] = folderInbox.search(new FlagTerm(new Flags(
			// Flags.Flag.SEEN), false));
			// logger.debug("No. of Unread Messages : " + messages.length);

			for (int i = 0; i < arrayMessages.length; i++) {
				IMAPMessage message = (IMAPMessage) arrayMessages[i];

				Address[] fromAddress = message.getFrom();
				String from = "";
				if (fromAddress != null && fromAddress.length > 0)
					from = fromAddress[0].toString();
				String subject = message.getSubject();
				Date sentDate = message.getSentDate();

				String contentType = message.getContentType();
				String messageContent = "";

				// store attachment file name, separated by comma
				String attachFiles = "";

				Email email = new Email(message.getMessageID(), from, subject, sentDate, messageContent);

				if (contentType.contains("multipart")) {
					// content may contain attachments
					Multipart multiPart = (Multipart) message.getContent();
					int numberOfParts = multiPart.getCount();
					for (int partCount = 0; partCount < numberOfParts; partCount++) {
						MimeBodyPart part = (MimeBodyPart) multiPart.getBodyPart(partCount);
						if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())) {
							// this part is attachment
							String fileName = part.getFileName();
							attachFiles += fileName + ", ";
							ByteArrayOutputStream bos = new ByteArrayOutputStream();
							part.getDataHandler().writeTo(bos);

							String decodedContent = bos.toString();
							Attachment attachment = new Attachment();
							attachment.setAttachmentType(AttachmentType.TEXT);
							attachment.setPayload(decodedContent);
							attachment.setName(fileName);
							email.getAttachments().add(attachment);

						} else {
							// this part may be the message content
							messageContent = part.getContent().toString();
						}
					}

					if (attachFiles.length() > 1) {
						attachFiles = attachFiles.substring(0, attachFiles.length() - 2);
					}
				} else if (contentType.contains("text/plain") || contentType.contains("text/html")) {
					Object content = message.getContent();
					if (content != null) {
						messageContent = content.toString();
					}
				}

				emails.add(email);

				// print out details of each message
				logger.info("Message #" + (i + 1) + ":");
				logger.debug("\t From: " + from);
				logger.debug("\t Subject: " + subject);
				logger.debug("\t Sent Date: " + sentDate);
				logger.debug("\t Message: " + messageContent);
				logger.debug("\t Attachments: " + attachFiles);

			}

			// disconnect
			folderInbox.close(false);
			store.close();
		} catch (IOException | MessagingException | ClassNotFoundException ex) {
			logger.debug("Exception:", ex);
		}

		return emails;
	}

}
