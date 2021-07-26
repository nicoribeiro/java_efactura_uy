package com.bluedot.commons.serializers;

import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.security.Attachment;

public class AttachmentSerializer<T> extends JSONSerializer<Attachment> {

	@Override
	public JSONObject objectToJson(Attachment attachment, boolean shrinkSerializarion) throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("id", attachment.getId());
		
		if (!shrinkSerializarion) {
			json.put("estado", attachment.getEstado().name());
			json.put("timestamp", attachment.getTimestamp());
			json.put("name", attachment.getName());
		}
		
		return json;
	}

}
