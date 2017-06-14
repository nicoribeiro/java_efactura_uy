package com.bluedot.commons.utils.messaging;

import java.util.LinkedList;
import java.util.List;

import javax.mail.MessagingException;

import play.Play;

public class EmailBuilder {

	private String to;
	private String textBody;
	private String htmlBody;
	private String subject;
	private boolean html;
	private String signature;
	private List<Attachment> attachments;
	private String imageUrl;
	private String logoUrl;
	private String mainColor;
	private String from;
	private String htmlTemplate = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\r\n<html xmlns=\"http://www.w3.org/1999/xhtml\" xmlns=\"http://www.w3.org/1999/xhtml\" style=\"font-family: 'HelveticaNeue-Light', Helvetica, Arial, sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\">&#13;\r\n<head>&#13;\r\n<meta name=\"viewport\" content=\"width=device-width\" />&#13;\r\n<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" />&#13;\r\n<title>Actionable emails e.g. reset password</title>&#13;\r\n&#13;\r\n&#13;\r\n&#13;\r\n</head>&#13;\r\n&#13;\r\n<body itemscope=\"\" itemtype=\"http://schema.org/EmailMessage\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; -webkit-font-smoothing: antialiased; -webkit-text-size-adjust: none; width: 100% !important; height: 100%; line-height: 1.6em; background-color: #f6f6f6; margin: 0; padding: 0;\" bgcolor=\"#f6f6f6\">&#13;\r\n&#13;\r\n<table class=\"body-wrap\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; width: 100%; background-color: #ffffff; margin: 0; margin-top: 20px;\" bgcolor=\"#f6f6f6\"><tr style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><td style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0;\" valign=\"top\"></td>&#13;\r\n\t\t<td class=\"container\" width=\"600\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; display: block !important; clear: both !important; width: 100% !important; margin: 0 auto; padding: 0;\" valign=\"top\">&#13;\r\n\t\t\t<div class=\"content\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; width: 90%; display: block; margin: 0 auto; padding: 0;\"><div style=\"text-align:right\"><img src=\"::logoUrl\" height=\"35\"></div>&#13;\r\n\t\t\t\t<table class=\"main\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" itemprop=\"action\" itemscope=\"\" itemtype=\"http://schema.org/ConfirmAction\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; background-color: #fff; margin: 0; border-bottom: 1px dashed #ddd;border-top: 5px solid ::brand1;\" bgcolor=\"#fff\"><tr style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><td class=\"content-wrap\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0; padding: 10px;\" valign=\"top\">&#13;\r\n\t\t\t\t\t\t\t<meta itemprop=\"name\" content=\"Confirm Email\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\" /><table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><tr style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><td class=\"content-block\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0; padding: 0 0 20px; color:#555 !important\" valign=\"top\">&#13;\r\n\t\t\t\t\t\t\t\t\t\t::body</td>&#13;\r\n\t\t\t\t\t\t\t\t</tr><tr style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><td class=\"content-block\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0; padding: 0 0 20px;color: black !important;\" valign=\"top\">&#13;\r\n\t\t\t\t\t\t\t\t\t\t\u2014 ::signature&#13;\r\n\t\t\t\t\t\t\t\t\t</td>&#13;\r\n\t\t\t\t\t\t\t\t</tr></table></td>&#13;\r\n\t\t\t\t\t</tr></table><div class=\"footer\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; width: 100%; clear: both; color: #999; margin: 0; padding: 20px;\">&#13;\r\n\t\t\t\t\t<table width=\"100%\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><tr style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; margin: 0;\"><td class=\"aligncenter content-block\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 12px; vertical-align: top; color: #999; text-align: center; margin: 0; padding: 0 0 20px;\" align=\"center\" valign=\"top\">Powered by <a href=\"http://behome247.com\" style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 12px; color: #999 !important; text-decoration: underline; margin: 0;\">BeHome247</a>.</td>&#13;\r\n\t\t\t\t\t\t</tr></table></div></div>&#13;\r\n\t\t</td>&#13;\r\n\t\t<td style=\"font-family: 'HelveticaNeue-Light',Helvetica,Arial,sans-serif; box-sizing: border-box; font-size: 14px; vertical-align: top; margin: 0;\" valign=\"top\"></td>&#13;\r\n\t</tr></table></body>&#13;\r\n</html>\r\n"; 

	public EmailBuilder() {
		attachments = new LinkedList<Attachment>();
	}

	public EmailBuilder withTo(String to) {
		this.to = to;
		return this;
	}

	public EmailBuilder withTextBody(String textBody) {
		this.textBody = textBody;
		return this;
	}

	public EmailBuilder withHtmlBody(String htmlBody) {
		this.htmlBody = htmlBody;
		return this;
	}

	public EmailBuilder withSubject(String subject) {
		this.subject = subject;
		return this;
	}

	public EmailBuilder withHtml(boolean html) {
		this.html = html;
		return this;
	}

	public EmailBuilder withSignature(String signature) {
		this.signature = signature;
		return this;
	}

	public EmailBuilder withAttachment(Attachment attachment) {
		this.attachments.add(attachment);
		return this;
	}
	
	public EmailBuilder withAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
		return this;
	}
	
	public EmailBuilder withImage(String imageUrl) {
		this.imageUrl = imageUrl;
		return this;
	}

	public EmailBuilder withLogoUrl(String logoUrl) {
		this.logoUrl = logoUrl;
		return this;
	}

	public EmailBuilder withMainColor(String mainColor) {
		this.mainColor = mainColor;
		return this;
	}

	public EmailBuilder withFrom(String from) {
		this.from = from;
		return this;
	}
	
	public EmailBuilder withHtmlTemplate(String htmlTemplate) {
		this.htmlTemplate = htmlTemplate;
		return this;
	}

	public boolean sendEmail() {
		String fromResolved = from != null ? from : Play.application().configuration().getString("mail.from", "");
		String host = Play.application().configuration().getString("mail.host", "");
		String port = Play.application().configuration().getString("mail.port", "");
		String username = Play.application().configuration().getString("mail.user", "");
		String password = Play.application().configuration().getString("mail.password", "");

		StringBuilder mailBody = new StringBuilder(htmlBody);

		if (html && imageUrl != null && imageUrl.endsWith("jpg")) {
			String img = "<br><img src=\"" + imageUrl + "\"><br>";
			mailBody.append(img);
		}

		if (signature == null || "".equals(signature))
			signature = Play.application().configuration().getString("mail.signature");

		htmlTemplate = brandEmail(htmlTemplate, logoUrl, mainColor);
		htmlTemplate = htmlTemplate.replace("::body", mailBody.toString().replace("\n", "<br>"));
		htmlTemplate = htmlTemplate.replace("::signature", html ? signature.replace("\n", "<br>") : signature);

		boolean logInstedOfSend = Play.application().configuration().getBoolean("mail.log", false);
		
		try {
			SendMail.sendMail(username, password, host, port, fromResolved, to, subject, textBody, htmlTemplate, attachments,
					logInstedOfSend);
		} catch (MessagingException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private String brandEmail(String htmlTemplate, String logoUrl, String mainColor) {
		htmlTemplate = htmlTemplate.replace("::brand1", mainColor != null ? mainColor : "#e6e6e6");
		htmlTemplate = htmlTemplate.replace("::logoUrl",
				logoUrl != null ? logoUrl : Play.application().configuration().getString("mail.logo", ""));
		return htmlTemplate;
	}

}
