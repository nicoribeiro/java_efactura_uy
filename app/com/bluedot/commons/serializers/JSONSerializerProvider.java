package com.bluedot.commons.serializers;

import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.commons.security.Session;
import com.bluedot.commons.security.Settings;

public class JSONSerializerProvider {

	public static JSONSerializer<Settings> getSettingsSerializer() {
		return new SettingsSerializer<Settings>();
	}

	public static JSONSerializer<Session> getSessionSerializer() {
		return new SessionSerializer<Session>();
	}
	
	public static JSONSerializer<EmailMessage> getEmailMessageSerializer() {
		return new EmailMessageSerializer<EmailMessage>(getAttachmentSerializer());
	}
	
	public static JSONSerializer<Attachment> getAttachmentSerializer() {
		return new AttachmentSerializer<Attachment>();
	}

}
