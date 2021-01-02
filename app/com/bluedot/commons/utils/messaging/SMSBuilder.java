package com.bluedot.commons.utils.messaging;

import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;
import com.twilio.sdk.resource.factory.MessageFactory;
import com.twilio.sdk.resource.instance.Message;

import play.Play;

public class SMSBuilder {
	
	final static Logger logger = LoggerFactory.getLogger(SMSBuilder.class);
	
	private String to;
	private String body;
	private String subject;
	private String signature;
	private String imageUrl;
	
	public SMSBuilder withTo(String to) {
		this.to = to;
		return this;
	}

	public SMSBuilder withBody(String body) {
		this.body = body;
		return this;
	}
	
	public SMSBuilder withSubject(String subject) {
		this.subject = subject;
		return this;
	}
	
	public SMSBuilder withSignature(String signature) {
		this.signature = signature;
		return this;
	}
	
	public SMSBuilder withImage(String imageUrl) {
		this.imageUrl = imageUrl;
		return this;
	}
	

	public boolean sendSMS()
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
		
		params.add(new BasicNameValuePair("Body", body.replaceAll("\\<.*?>","")));
		
		if(imageUrl != null && imageUrl.endsWith("jpg"))
			params.add(new BasicNameValuePair("MediaUrl", imageUrl));

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
				logger.info("IMAGE_URL: " + imageUrl);
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
