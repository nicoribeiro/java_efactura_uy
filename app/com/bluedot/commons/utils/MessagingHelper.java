package com.bluedot.commons.utils;

import java.util.Map;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

public class MessagingHelper {
	final static Logger logger = LoggerFactory.getLogger(MessagingHelper.class);

	private String from;
	private String host;
	private int port;
	private String username;
	private String password;

	private String signature;
	private Map<String, byte[]> attachments;

	/** Loguear en lugar de mandar; null = segun mail.log */
	private Boolean soloLog;

	public MessagingHelper() {

	}

	public MessagingHelper withPlayConfig() {
		from = Play.application().configuration().getString("mail.from", "");
		host = Play.application().configuration().getString("mail.host", "");
		port = Play.application().configuration().getInt("mail.port", 25);
		username = Play.application().configuration().getString("mail.user", "");
		password = Play.application().configuration().getString("mail.password", "");
		return this;
	}

	public MessagingHelper withCustomConfig(String from, String host, int port, String username, String password) {
		this.from = from;
		this.host = host;
		this.port = port;
		this.username = username;
		this.password = password;
		return this;
	}

	public MessagingHelper withAttachment(Map<String, byte[]> attachments) {
		this.attachments = attachments;
		return this;
	}

	/** Loguear en lugar de mandar, sin importar mail.log (ej. los mails del sistema tienen su propia opcion) */
	public MessagingHelper withSoloLog(boolean soloLog) {
		this.soloLog = soloLog;
		return this;
	}

	public MessagingHelper withSignature(String signature) {
		this.signature = signature;
		return this;
	}

	public String getValidationHost(String host) {
		return Play.application().configuration().getString("app.validation.host", "https://" + host);
	}

	public boolean sendEmail(String to, String textBody, String htmlBody, String subject, boolean html) {

		if (signature == null || "".equals(signature))
			signature = Play.application().configuration().getString("mail.signature", "");

		boolean logInstedOfSend = soloLog != null ? soloLog : Play.application().configuration().getBoolean("mail.log", false);

		try {
			SendMail.sendMail(username, password, host, port, from, to, subject, textBody, null, attachments,
					logInstedOfSend);
		} catch (MessagingException e) {
			logger.error("Exception:", e);
			return false;
		}

		return true;
	}
}
