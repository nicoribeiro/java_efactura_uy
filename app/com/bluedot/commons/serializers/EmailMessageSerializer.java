package com.bluedot.commons.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.Attachment;
import com.bluedot.commons.security.EmailMessage;
import com.bluedot.efactura.model.CFE;
import com.bluedot.efactura.model.Empresa;
import com.bluedot.efactura.model.Respuesta;

public class EmailMessageSerializer<T> extends JSONSerializer<EmailMessage> {

	JSONSerializer<Attachment> attachmentSerializer;
	
	public EmailMessageSerializer(JSONSerializer<Attachment> attachmentSerializer) {
		this.attachmentSerializer = attachmentSerializer;
	}
	
	@Override
	public JSONObject objectToJson(EmailMessage emailMessage, boolean shrinkSerializarion) throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("id", emailMessage.getId());
		
		if (!shrinkSerializarion) {
			json.put("messageId", emailMessage.getMessageId());
			json.put("sentDate", emailMessage.getSentDate());
			json.put("subject", emailMessage.getSubject());
			json.put("attachments", attachmentSerializer.objectToJson(emailMessage.getAttachments(), false));
		}
		
		return json;
	}

}
