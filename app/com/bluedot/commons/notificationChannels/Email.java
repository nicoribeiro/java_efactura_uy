package com.bluedot.commons.notificationChannels;

import java.util.UUID;

import javax.persistence.Entity;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.messages.Message;

import play.i18n.Messages;

@Entity
public class Email extends NotificationChannel
{

	private String email;

	
	public Email(){
		
	}
	
	public Email(String description, String email) {
		super(description);
		this.email = email;
	}

	@Override
	public void sendNotification(Alert alert)
	{
		if (getValidated() && getEnabled())
		{

			StringBuilder mailBody = new StringBuilder(alert.getBody());
			String subject = alert.getSubject();
			MessagingHelper.sendEmail(email, mailBody.toString(), mailBody.toString().replace("\n", "<br>"), subject, true, alert.getAttachment());
		}
	}

	@Override
	public void test()
	{
		if (getValidated() && getEnabled())
		{ 
			StringBuilder mailBody = new StringBuilder(Messages.get("test_mail"));
			String subject = "Mail Test";
			MessagingHelper.sendEmail(email, mailBody.toString(), mailBody.toString(), subject, false, null);
		}
		
	}

	@Override
	public void sendValidationKey(String host)
	{
		String activationLink = host+"/api/v1/notificationChannels/"+getId()+"/validate/"+getValidationKey();
		
		
		StringBuilder htmlEmailBody = new StringBuilder();
		
		htmlEmailBody.append("<html>");
		htmlEmailBody.append("<body>");
		htmlEmailBody.append("<p>Validate this Channel <a href=\""+activationLink+"\">here</a></p>");
		htmlEmailBody.append("</body>");
		htmlEmailBody.append("</html>");
		
		StringBuilder textEmailBody = new StringBuilder();
		
		textEmailBody.append("Validate this Channel by accessing this URL: "+activationLink);
		
		String subject = "Mail Test";
		MessagingHelper.sendEmail(email, textEmailBody.toString(), htmlEmailBody.toString(), subject, true, null);
		
	}

	public String getEmail()
	{
		return email;
	}

	public void setEmail(String email)
	{
		this.email = email;
	}

	@Override
	protected String generateValidationKey()
	{
		return UUID.randomUUID().toString();
	}

	@Override
	public void sendMessage(Message message)
	{
		if (getValidated() && getEnabled())
		{
			String subject = Messages.get("messages_new_message_title");
			MessagingHelper.sendEmail(email, message.getMessage(), message.getMessage().replace("\n", "<br>"), subject, true, message.getFromString());
		}
		
	}


}
