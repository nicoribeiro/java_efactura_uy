package com.bluedot.commons.messages;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.security.Account;
import com.bluedot.commons.security.User;



public class MessageHelper
{

	/**
	 * Method that maps a lis of urn to a list of MessageReceivers
	 * @param URNs a list of URN. URN format is: "class_name":"id"
	 * @return a list of MessageReceivers that map those URN
	 */
	public static List<MessageReceiver> getMessageReceivers(List<String> urns)
	{
		List<MessageReceiver> result = new LinkedList<MessageReceiver>();

		for (Iterator<String> iterator = urns.iterator(); iterator.hasNext();)
		{
			String converseId = iterator.next();
			MessageReceiver receiver = getMessageReceiver(converseId);
			
			if (receiver!=null){
				result.add(receiver);
			}

		}
		return result;
	}

	/**
	 * Method that map an URN to a MessageReceiver
	 * @param urn MessageReceiver identifier: "class_name":"id"
	 * @return A MeesageReceiver instance witch id == "id" && class == "class_name"
	 */
	public static MessageReceiver getMessageReceiver(String urn)
	{
		String id = urn.split(":")[1];
		if (urn.startsWith("account"))
			return Account.findById(Integer.parseInt(id));
		if (urn.startsWith("user"))
			return User.findById(Integer.parseInt(id));
//		if (urn.startsWith("reservation"))
//			return Reservation.findById(Long.parseLong(id));
//		if (urn.startsWith("guest"))
//			return Guest.findById(Integer.parseInt(id));
		
		return null;
	}
	

	public static List<MessageReceiver> getMessageReceivers(JSONArray receivers) throws JSONException
	{
		List<MessageReceiver> result = new LinkedList<MessageReceiver>();

		for (int i=0; i<receivers.length();i++)
		{
			JSONObject destination = receivers.getJSONObject(i);

			String name = JSONObject.getNames(destination)[0];
			String value = destination.getString(name);
			String dest = name + ":" + value;
			
			
			MessageReceiver receiver = getMessageReceiver(dest);
			
			if (receiver!=null){
				result.add(receiver);
			}

		}
		return result;
	}
	

}
