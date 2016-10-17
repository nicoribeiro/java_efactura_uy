package com.bluedot.commons.notificationChannels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.utils.SendMail;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

import play.Play;

public class MessagingHelper
{
	final static Logger logger = LoggerFactory.getLogger(MessagingHelper.class);
	
	public static String getValidationHost(String host)
	{
		return Play.application().configuration().getString("app.validation.host", "https://" + host);
	}
	
	public static boolean sendEmail(String to, String textBody, String htmlBody, String subject, boolean html)
	{
		return sendEmail(to, textBody, htmlBody, subject, html, null);
	}
	
	public static boolean sendEmail(String to, String textBody, String htmlBody, String subject, boolean html, String signature)
	{
		return sendEmail(to, textBody, htmlBody, subject, html, signature, null);
	}
	
	//TODO se puede hacer un builder con with(attachment) y demas config para eliminar tantos metodos parecidos
	public static boolean sendEmail(String to, String textBody, String htmlBody, String subject, boolean html, String signature, Map<String, String> attachments)
	{
		String from = Play.application().configuration().getString("mail.from", "");
		String host = Play.application().configuration().getString("mail.host", "");
		String port = Play.application().configuration().getString("mail.port", "");
		String username = Play.application().configuration().getString("mail.user", "");
		String password = Play.application().configuration().getString("mail.password", "");

//		StringBuilder mailBody = new StringBuilder("<html><body>");
//		mailBody.append(htmlBody);
		
//		if(html && attachment != null && attachment.endsWith("jpg"))
//		{
//			String img = "<br><img src=\""+attachment+"\"><br>";
//			mailBody.append(img);
//		}
		
		if(signature == null || "".equals(signature))
			signature = Play.application().configuration().getString("mail.signature", "");
		
//		mailBody.append(html?signature.replace("\n", "<br>"):signature);
//		mailBody.append("</body></html>");

		boolean logInstedOfSend = Play.application().configuration().getBoolean("mail.log", false);
		try
		{
			SendMail.sendMail(username, password, host, port, from, to, subject, textBody, null, attachments, logInstedOfSend);
		} catch (MessagingException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public static boolean sendSMS(String to, String body, String subject, String signature)
	{
		return sendSMS(to, body, subject, signature, null);
	}

	public static boolean sendSMS(String to, String body, String subject, String signature, String attachment)
	{
		String from = Play.application().configuration().getString("sms.from", "");
		String sid = Play.application().configuration().getString("sms.app.sid", "");
		String token = Play.application().configuration().getString("sms.app.token", "");

		boolean logInstedOfSend = Play.application().configuration().getBoolean("sms.log", false);

		TwilioRestClient client = new TwilioRestClient(sid, token);

		// Build the parameters
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("To", to));
		params.add(new BasicNameValuePair("From", from));
		
		if(signature != null && !"".equals(signature))
			body = "["+signature+"]: " + body;
		
		params.add(new BasicNameValuePair("Body", body));
		
		if(attachment != null && attachment.endsWith("jpg"))
			params.add(new BasicNameValuePair("MediaUrl", attachment));

		MessageFactory messageFactory = client.getAccount().getMessageFactory();
		Message message;
		try
		{
			if(logInstedOfSend)
			{
				logger.info(">>>> SMS DUMP:");
				logger.info("FROM: " + from);
				logger.info("SUBJECT: " + subject);
				logger.info("TO: " + to);
				logger.info("BODY: " + body);
				logger.info("ATTACHMENT: " + attachment);
			}else
			{
				message = messageFactory.create(params);
				logger.info("SMS sent correctly to: " + to + ", sid: " + message.getSid());
			}
		} catch (TwilioRestException e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
